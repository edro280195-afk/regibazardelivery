package com.example.regibazarapp.ui.utils

import com.google.android.gms.maps.model.LatLng
import com.google.gson.annotations.SerializedName
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import java.util.concurrent.TimeUnit
import android.util.Log

/**
 * ═══════════════════════════════════════════════════════════
 *  DIRECTIONS MANAGER – Google Maps Directions API
 *
 *  Obtiene la polilínea de conducción entre dos puntos
 *  usando la API REST de Directions de Google Maps.
 *  Decodifica la polilínea encoded (algoritmo estándar)
 *  y expone los puntos LatLng para dibujar en el mapa.
 * ═══════════════════════════════════════════════════════════
 */

// ─── Data classes para parsing de la respuesta ───

data class DirectionsResponse(
    @SerializedName("status") val status: String,
    @SerializedName("routes") val routes: List<DirectionsRoute>
)

data class DirectionsRoute(
    @SerializedName("overview_polyline") val overview_polyline: DirectionsPolyline,
    @SerializedName("legs") val legs: List<DirectionsLeg>
)

data class DirectionsPolyline(
    @SerializedName("points") val points: String
)

data class DirectionsLeg(
    @SerializedName("duration") val duration: DirectionsValue,
    @SerializedName("distance") val distance: DirectionsValue
)

data class DirectionsValue(
    @SerializedName("text") val text: String,
    @SerializedName("value") val value: Int
)

// ─── Resultado expuesto al caller ───

data class RouteInfo(
    val durationText: String,
    val distanceText: String,
    val points: List<LatLng>
)

// ─── Manager singleton ───

object DirectionsManager {

    private val API_KEY = com.example.regibazarapp.BuildConfig.MAPS_API_KEY
    private const val BASE_URL = "https://maps.googleapis.com/maps/api/directions/json"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private val routeCache = java.util.concurrent.ConcurrentHashMap<String, RouteInfo>()

    private fun buildCacheKey(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double,
        waypoints: List<LatLng>
    ): String {
        val oStr = String.format(java.util.Locale.US, "%.4f,%.4f", originLat, originLng)
        val dStr = String.format(java.util.Locale.US, "%.4f,%.4f", destLat, destLng)
        val wStr = waypoints.joinToString(";") {
            String.format(java.util.Locale.US, "%.4f,%.4f", it.latitude, it.longitude)
        }
        return "$oStr->$wStr->$dStr"
    }

    /**
     * Obtiene la ruta en auto entre origen y destino.
     * Retorna [RouteInfo] con texto de duración, distancia
     * y lista de [LatLng] para trazar la polilínea.
     * Utiliza caché en memoria para proteger los costos de la API de Google Maps.
     */
    suspend fun getRoutePoints(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double,
        waypoints: List<LatLng> = emptyList()
    ): RouteInfo? = withContext(Dispatchers.IO) {
        val cacheKey = buildCacheKey(originLat, originLng, destLat, destLng, waypoints)
        routeCache[cacheKey]?.let { cached ->
            Log.d("DirectionsManager", "⚡ Serving route from cache: $cacheKey (${cached.points.size} points)")
            return@withContext cached
        }

        try {
            val url = Uri.parse(BASE_URL).buildUpon()
                .appendQueryParameter("origin", "$originLat,$originLng")
                .appendQueryParameter("destination", "$destLat,$destLng")
                .apply {
                    if (waypoints.isNotEmpty()) {
                        appendQueryParameter("waypoints", waypoints.joinToString("|") { "${it.latitude},${it.longitude}" })
                    }
                }
                .appendQueryParameter("mode", "driving")
                .appendQueryParameter("key", API_KEY)
                .build()
                .toString()

            Log.d("DirectionsManager", "Request URL: $url")

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            Log.d("DirectionsManager", "Response Body: $body")

            val directionsResponse = gson.fromJson(body, DirectionsResponse::class.java)

            if (directionsResponse.status != "OK") {
                Log.e("DirectionsManager", "API Error Status: ${directionsResponse.status}")
                return@withContext null
            }
            if (directionsResponse.routes.isEmpty()) return@withContext null

            val route = directionsResponse.routes.first()
            val encodedPolyline = route.overview_polyline.points
            val points = decodePolyline(encodedPolyline)
            Log.d("DirectionsManager", "Decoded ${points.size} points")

            if (points.isEmpty()) {
                Log.w("DirectionsManager", "No points decoded from polyline")
                return@withContext null
            }

            val leg = route.legs.firstOrNull()
            Log.d("DirectionsManager", "Route info: ${leg?.duration?.text}, ${leg?.distance?.text}")

            val info = RouteInfo(
                durationText = leg?.duration?.text ?: "",
                distanceText = leg?.distance?.text ?: "",
                points = points
            )
            routeCache[cacheKey] = info
            info
        } catch (e: Exception) {
            Log.e("DirectionsManager", "getRoutePoints exception: ${e.message}", e)
            null
        }
    }

    /**
     * Obtiene la polilínea continua por calles para una secuencia de paradas en el mapa.
     * Utiliza caché por tramos entre paradas consecutivas para minimizar las peticiones a la API.
     */
    suspend fun getMultiStopPolyline(stops: List<LatLng>): List<LatLng> = withContext(Dispatchers.IO) {
        if (stops.size < 2) return@withContext emptyList()

        if (stops.size <= 8) {
            val origin = stops.first()
            val dest = stops.last()
            val waypoints = stops.subList(1, stops.size - 1)
            val info = getRoutePoints(origin.latitude, origin.longitude, dest.latitude, dest.longitude, waypoints)
            if (info != null && info.points.isNotEmpty()) {
                return@withContext info.points
            }
        }

        // Fallback por tramos cacheados si son más de 8 o si falla la petición completa
        val fullPolyline = mutableListOf<LatLng>()
        for (i in 0 until stops.size - 1) {
            val from = stops[i]
            val to = stops[i + 1]
            val info = getRoutePoints(from.latitude, from.longitude, to.latitude, to.longitude)
            if (info != null && info.points.isNotEmpty()) {
                if (fullPolyline.isNotEmpty()) {
                    fullPolyline.addAll(info.points.drop(1))
                } else {
                    fullPolyline.addAll(info.points)
                }
            } else {
                if (fullPolyline.isEmpty()) fullPolyline.add(from)
                fullPolyline.add(to)
            }
        }
        fullPolyline
    }

    /**
     * Decodifica una polilínea Google encoded al algoritmo estándar.
     *
     * El algoritmo acumula offsets de latitud y longitud codificados
     * en bloques de 5 bits con inversión de signo para valores negativos.
     *
     * Referencia: https://developers.google.com/maps/documentation/utilities/polylinealgorithm
     */
    fun decodePolyline(encoded: String): List<LatLng> {
        val result = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            // Decodificar latitud
            var b: Int
            var shift = 0
            var resultLat = 0
            do {
                b = encoded[index++].code - 63
                resultLat = resultLat or ((b and 0x1F) shl shift)
                shift += 5
            } while (b >= 0x20)

            val deltaLat = if ((resultLat and 1) != 0) (resultLat shr 1).inv() else resultLat shr 1
            lat += deltaLat

            // Decodificar longitud
            shift = 0
            var resultLng = 0
            do {
                b = encoded[index++].code - 63
                resultLng = resultLng or ((b and 0x1F) shl shift)
                shift += 5
            } while (b >= 0x20)

            val deltaLng = if ((resultLng and 1) != 0) (resultLng shr 1).inv() else resultLng shr 1
            lng += deltaLng

            result.add(LatLng(lat / 1e5, lng / 1e5))
        }

        return result
    }
}
