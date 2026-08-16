package com.example.regibazarapp.data.network

import com.example.regibazarapp.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * ══════════════════════════════════════════════════
 *  Network Module – Singleton Retrofit & OkHttp
 * ══════════════════════════════════════════════════
 */
object NetworkModule {

    private const val BASE_URL = "https://regibazarapi.onrender.com/api/"
    const val SIGNALR_HUB_URL = "https://regibazarapi.onrender.com/hubs/delivery"

    // JWT token para el admin API — se actualiza tras login
    @Volatile
    var jwtToken: String? = null

    // Solo log completo en builds de debug — en producción no se exponen tokens ni cuerpos
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val original = chain.request()
            // Si hay un token explícito en el header, no sobreescribir (permite flexibilidad)
            if (original.header("Authorization") != null) {
                return@addInterceptor chain.proceed(original)
            }
            
            val request = jwtToken?.let { token ->
                original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } ?: original
            chain.proceed(request)
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Redundante pero mantenido para compatibilidad de tipos si es necesario
    private val adminOkHttpClient = okHttpClient

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val adminRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(adminOkHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: RegiBazarApi = retrofit.create(RegiBazarApi::class.java)
    val adminApi: AdminApi = adminRetrofit.create(AdminApi::class.java)
}
