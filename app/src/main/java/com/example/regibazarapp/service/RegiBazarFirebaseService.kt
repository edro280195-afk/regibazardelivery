package com.example.regibazarapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.regibazarapp.MainActivity
import com.example.regibazarapp.R
import com.example.regibazarapp.data.network.NetworkModule
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ══════════════════════════════════════════════════════════
 *  RegiBazar Firebase Messaging Service
 *
 *  Maneja dos eventos:
 *  1. onNewToken  — el dispositivo recibió un nuevo token FCM.
 *                   Lo registra en la API con role="driver".
 *  2. onMessageReceived — llega un mensaje FCM mientras la app
 *                   está en primer plano. Muestra una notificación local.
 *
 *  Cuando la app está en segundo plano / cerrada, FCM muestra
 *  automáticamente la notificación usando el payload "notification".
 * ══════════════════════════════════════════════════════════
 */
class RegiBazarFirebaseService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "regibazar_channel"
        const val CHANNEL_NAME = "RegiBazar"
        private const val PREF_FCM_TOKEN = "fcm_token"

        /** Registra el token FCM actual con la API. Llama esto al hacer login. */
        fun registerCurrentToken(context: Context, driverRouteToken: String? = null) {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        NetworkModule.api.subscribeFcm(
                            FcmSubscribeRequest(
                                fcmToken = token,
                                role = "driver",
                                driverRouteToken = driverRouteToken
                            )
                        )
                        // Guardar localmente para poder hacer unsubscribe en logout
                        context.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
                            .edit().putString(PREF_FCM_TOKEN, token).apply()
                    } catch (e: Exception) {
                        // No es crítico — se reintentará en el siguiente login
                    }
                }
            }
        }

        /** Llama esto al hacer logout para limpiar el token en la API. */
        fun unregisterToken(context: Context) {
            val token = context.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
                .getString(PREF_FCM_TOKEN, null) ?: return
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    NetworkModule.api.unsubscribeFcm(token)
                    context.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
                        .edit().remove(PREF_FCM_TOKEN).apply()
                } catch (_: Exception) {}
            }
        }
    }

    // ─── Token refresh ───

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Guardar localmente
        getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
            .edit().putString(PREF_FCM_TOKEN, token).apply()
        // Registrar en API
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NetworkModule.api.subscribeFcm(
                    FcmSubscribeRequest(fcmToken = token, role = "driver")
                )
            } catch (_: Exception) {}
        }
    }

    // ─── Mensaje en primer plano ───

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"]
        val body = message.notification?.body ?: message.data["body"]
        val driverToken = message.data["driverToken"]
        val action = message.data["action"]

        // 📡 SILENT REFRESH: Si viene la acción de refresco, notificamos al SyncManager
        if (action == "REFRESH_ROUTE") {
            com.example.regibazarapp.data.SyncManager.triggerRefresh()
        }

        // Solo mostrar notificación si tiene contenido visual
        if (!title.isNullOrBlank() || !body.isNullOrBlank()) {
            showLocalNotification(title ?: "RegiBazar", body ?: "", driverToken)
        }
    }

    // ─── Notificación local ───

    private fun showLocalNotification(title: String, body: String, driverToken: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal (requerido API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de entregas y rutas"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent que abre la app (y navega a la ruta si hay token)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!driverToken.isNullOrBlank()) {
                putExtra("driver_token_deeplink", driverToken)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_splash_icon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 200, 100, 200))
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

/** DTO para registrar el token FCM en la API */
data class FcmSubscribeRequest(
    val fcmToken: String,
    val role: String = "driver",
    val driverRouteToken: String? = null
)
