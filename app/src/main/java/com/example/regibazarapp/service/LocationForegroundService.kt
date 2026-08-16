package com.example.regibazarapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.regibazarapp.MainActivity
import com.example.regibazarapp.R
import com.example.regibazarapp.data.models.UpdateLocationRequest
import com.example.regibazarapp.data.network.NetworkModule
import android.os.PowerManager
import com.example.regibazarapp.data.TokenManager
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ═══════════════════════════════════════════════════════════════
 *  EL MOTOR INMORTAL – LocationForegroundService
 *
 *  GPS cada 10s → REST API + notificación enriquecida.
 *  Detecta proximidad (<200m) al destino y vibra/notifica.
 *  Sobrevive Doze Mode y pantalla bloqueada.
 * ═══════════════════════════════════════════════════════════════
 */
class LocationForegroundService : Service() {

    companion object {
        private const val TAG = "LocationService"
        private const val CHANNEL_ID = "regi_bazar_tracking"
        private const val CHANNEL_PROXIMITY_ID = "regi_bazar_proximity"
        private const val NOTIFICATION_ID = 1001
        private const val PROXIMITY_NOTIFICATION_ID = 1002
        const val EXTRA_DRIVER_TOKEN = "driver_token"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"

        // Compartido con RouteViewModel — actualizado por ViewModel al cambiar entrega
        @Volatile var currentDeliveryLat: Double? = null
        @Volatile var currentDeliveryLng: Double? = null
        @Volatile var currentDeliveryName: String = ""
        @Volatile var currentDeliveryId: Int = -1
        @Volatile var currentDeliveryInstructions: String? = null
        @Volatile var currentDeliveryBalance: Double = 0.0
        @Volatile var currentDeliveryPhone: String? = null
        @Volatile var currentDeliveryPaymentMethod: String? = null
        @Volatile var deliveredCount: Int = 0
        @Volatile var totalCount: Int = 0

        /** Callback invocado al detectar proximidad — RouteViewModel lo registra para Auto-TTS */
        var onProximityDetected: ((deliveryName: String, instructions: String?, balance: Double, phone: String?, paymentMethod: String?) -> Unit)? = null

        // Interfaz de reactividad para Compose
        private val _locationFlow = MutableStateFlow<Location?>(null)
        val locationFlow = _locationFlow.asStateFlow()

        // Última ubicación conocida (compatibilidad)
        @Volatile var lastLat: Double = 0.0
        @Volatile var lastLng: Double = 0.0
        @Volatile var hasLocation: Boolean = false

        private var lastProximityAlertedDeliveryId: Int = -1

        fun start(context: Context, driverToken: String) {
            val intent = Intent(context, LocationForegroundService::class.java).apply {
                putExtra(EXTRA_DRIVER_TOKEN, driverToken)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LocationForegroundService::class.java))
        }
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var notificationManager: NotificationManager
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var tokenManager: TokenManager
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var driverToken: String = ""

    // ── GPS adaptativo ──
    private var lastSentLat: Double = 0.0
    private var lastSentLng: Double = 0.0
    private var lastSentMs: Long = 0L

    // ─── LIFECYCLE ───

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannels()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        tokenManager = TokenManager(this)

        // Configurar WakeLock para evitar que el CPU duerma durante el envío de datos
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RegiBazar:LocationWakeLock")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_SERVICE) {
            Log.i(TAG, "🛑 Comando de detención recibido via Intent.")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        driverToken = intent?.getStringExtra(EXTRA_DRIVER_TOKEN) ?: ""

        if (driverToken.isEmpty()) {
            Log.e(TAG, "❌ No se recibió driver token. Deteniendo servicio.")
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildTrackingNotification())
        startLocationUpdates()

        Log.i(TAG, "🚀 Servicio iniciado para token: ${driverToken.take(8)}...")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.i(TAG, "🛑 Intentando detener servicio y limpiar notificaciones...")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopLocationUpdates()
        serviceScope.cancel()
        if (wakeLock.isHeld) wakeLock.release()
        super.onDestroy()
        Log.i(TAG, "🛑 Servicio destruido completamente.")
    }

    // ─── LOCATION TRACKING ───

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10_000L
        ).apply {
            setMinUpdateIntervalMillis(5_000L)
            setWaitForAccurateLocation(false)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    onNewLocation(location)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Permisos de ubicación no otorgados: ${e.message}")
        }
    }

    private fun stopLocationUpdates() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun onNewLocation(location: Location) {
        // Actualizar estados reactivos y estáticos
        lastLat = location.latitude
        lastLng = location.longitude
        hasLocation = true
        _locationFlow.value = location

        Log.d(TAG, "📍 Nueva coordenada: ${location.latitude}, ${location.longitude}")

        // ── GPS Adaptativo ──
        // Criterio 1: cerca del destino (<500m) → siempre enviar
        val destLat = currentDeliveryLat
        val destLng = currentDeliveryLng
        val nearDestination = if (destLat != null && destLng != null) {
            val dist = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude, destLat, destLng, dist)
            dist[0] < 500f
        } else false

        // Criterio 2: se movió >25m desde el último envío
        val distFromLastSent = if (lastSentMs > 0) {
            val d = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude, lastSentLat, lastSentLng, d)
            d[0]
        } else Float.MAX_VALUE

        // Criterio 3: han pasado >30s (cuando está estático)
        val secondsElapsed = (System.currentTimeMillis() - lastSentMs) / 1000L

        val shouldSend = nearDestination || distFromLastSent > 25f || secondsElapsed >= 30

        if (shouldSend) {
            lastSentLat = location.latitude
            lastSentLng = location.longitude
            lastSentMs = System.currentTimeMillis()
            sendLocationToApi(location.latitude, location.longitude)
            SignalRManager.getInstance().reportLocation(location.latitude, location.longitude)
        }

        // UI & Proximidad (siempre)
        updateNotification()
        checkProximity(location)
    }

    private fun sendLocationToApi(lat: Double, lng: Double) {
        serviceScope.launch {
            // Adquirir WakeLock brevemente para asegurar que el request termine
            if (!wakeLock.isHeld) wakeLock.acquire(3000L) // 3 seg max
            
            try {
                // Prioridad: Usar token de persistencia si el del intent falla
                val token = if (driverToken.isNotEmpty()) driverToken else tokenManager.getToken() ?: ""
                
                if (token.isNotEmpty()) {
                    NetworkModule.api.updateLocation(
                        driverToken = token,
                        request = UpdateLocationRequest(lat, lng)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error crítico enviando coordenadas: ${e.message}")
            }
        }
    }

    // ─── PROXIMIDAD ───

    private fun checkProximity(location: Location) {
        val destLat = currentDeliveryLat ?: return
        val destLng = currentDeliveryLng ?: return
        val deliveryId = currentDeliveryId

        if (deliveryId == lastProximityAlertedDeliveryId) return // Ya alertamos este

        val results = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude, destLat, destLng, results)
        val distanceMeters = results[0]

        if (distanceMeters < 200f) {
            Log.i(TAG, "📍 Proximidad detectada: ${distanceMeters.toInt()}m de $currentDeliveryName")
            lastProximityAlertedDeliveryId = deliveryId
            triggerProximityAlert()
        }
    }

    private fun triggerProximityAlert() {
        // Vibración
        vibrate()

        // Auto-TTS — notificar al ViewModel para que CAMI anuncie por Bluetooth
        onProximityDetected?.invoke(
            currentDeliveryName,
            currentDeliveryInstructions,
            currentDeliveryBalance,
            currentDeliveryPhone,
            currentDeliveryPaymentMethod
        )

        // Notificación rica de proximidad con detalles del cliente
        val pendingIntent = buildPendingIntent()
        val balanceText = if (currentDeliveryBalance > 0)
            "Cobrar: \$${String.format("%,.0f", currentDeliveryBalance)}"
        else "Sin saldo pendiente"
        val methodText = currentDeliveryPaymentMethod ?: "No definido"

        val builder = NotificationCompat.Builder(this, CHANNEL_PROXIMITY_ID)
            .setContentTitle("📍 ¡Llegando con $currentDeliveryName!")
            .setContentText("$balanceText · Pago: $methodText")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                buildString {
                    append("$balanceText · Método: $methodText")
                    if (!currentDeliveryInstructions.isNullOrBlank()) {
                        append("\n📋 $currentDeliveryInstructions")
                    }
                }
            ))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)

        // Acción de Llamar al cliente (si tiene teléfono)
        if (!currentDeliveryPhone.isNullOrBlank()) {
            val callIntent = PendingIntent.getActivity(
                this, 2,
                Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:$currentDeliveryPhone")),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                NotificationCompat.Action.Builder(null, "📞 Llamar", callIntent).build()
            )
        }

        notificationManager.notify(PROXIMITY_NOTIFICATION_ID, builder.build())
    }

    private fun vibrate() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(VibratorManager::class.java)
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 300, 100, 300), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Vibrator::class.java)
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 300, 100, 300), -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error vibrando: ${e.message}")
        }
    }

    // ─── NOTIFICACIÓN ENRIQUECIDA ───

    private fun createNotificationChannels() {
        // Canal tracking (silencioso, persistente)
        val trackingChannel = NotificationChannel(
            CHANNEL_ID,
            "Tracking de Ruta",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Seguimiento GPS activo durante la ruta"
            setShowBadge(false)
        }

        // Canal proximidad (con sonido y vibración)
        val proximityChannel = NotificationChannel(
            CHANNEL_PROXIMITY_ID,
            "Alertas de Proximidad",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alertas al llegar cerca del destino"
            enableVibration(true)
        }

        notificationManager.createNotificationChannel(trackingChannel)
        notificationManager.createNotificationChannel(proximityChannel)
    }

    private fun buildTrackingNotification(): Notification {
        val progressText = if (totalCount > 0)
            "Entrega $deliveredCount/$totalCount · ${buildDestinationText()}"
        else
            "Ruta Activa · Tracking GPS 📍"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RegiDelivery")
            .setContentText(progressText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(buildPendingIntent())
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setOnlyAlertOnce(true)
            .addAction(
                NotificationCompat.Action.Builder(
                    null,
                    "Pausar",
                    buildStopPendingIntent()
                ).build()
            )
            .build()
    }

    private fun updateNotification() {
        notificationManager.notify(NOTIFICATION_ID, buildTrackingNotification())
    }

    private fun buildDestinationText(): String {
        val name = currentDeliveryName
        return if (name.isNotEmpty()) "Siguiente: $name" else "GPS activo 📍"
    }

    private fun buildPendingIntent() = PendingIntent.getActivity(
        this, 0,
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("driver_token_deeplink", driverToken)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun buildStopPendingIntent() = PendingIntent.getService(
        this, 1,
        Intent(this, LocationForegroundService::class.java).apply {
            action = ACTION_STOP_SERVICE
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
