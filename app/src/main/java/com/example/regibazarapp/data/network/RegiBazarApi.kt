package com.example.regibazarapp.data.network

import com.example.regibazarapp.data.models.*
import com.example.regibazarapp.service.FcmSubscribeRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * ══════════════════════════════════════════════════════
 *  Retrofit API Interface – Driver Endpoints
 *  Base URL: https://regibazarapi.onrender.com/api/
 * ══════════════════════════════════════════════════════
 */
interface RegiBazarApi {

    // ─── RUTA ───

    /** Obtener la ruta completa del chofer */
    @GET("driver/{token}")
    suspend fun getRoute(
        @Path("token") driverToken: String
    ): Response<RouteResponse>

    /** Iniciar la ruta */
    @POST("driver/{token}/start")
    suspend fun startRoute(
        @Path("token") driverToken: String,
        @Body request: StartRouteRequest? = null
    ): Response<SimpleMessageResponse>

    // ─── GPS ───

    /** Actualizar ubicación del chofer */
    @POST("driver/{token}/location")
    suspend fun updateLocation(
        @Path("token") driverToken: String,
        @Body request: UpdateLocationRequest
    ): Response<Unit>

    /** Actualizar coordenadas del destino (geocoding local fallback) */
    @POST("driver/{token}/delivery/{deliveryId}/coordinates")
    suspend fun updateDeliveryCoordinates(
        @Path("token") driverToken: String,
        @Path("deliveryId") deliveryId: Int,
        @Body request: UpdateLocationRequest
    ): Response<SimpleMessageResponse>

    // ─── ENTREGAS ───

    /** Marcar entrega como en tránsito */
    @POST("driver/{token}/transit/{deliveryId}")
    suspend fun markInTransit(
        @Path("token") driverToken: String,
        @Path("deliveryId") deliveryId: Int
    ): Response<SimpleMessageResponse>

    /** Marcar entrega como completada (multipart con fotos) */
    @Multipart
    @POST("driver/{token}/deliver/{deliveryId}")
    suspend fun markDelivered(
        @Path("token") driverToken: String,
        @Path("deliveryId") deliveryId: Int,
        @Header("X-Idempotency-Key") idempotencyKey: String,
        @Part("notes") notes: RequestBody?,
        @Part("payments") payments: RequestBody?,
        @Part("paymentsJson") paymentsJson: RequestBody?,
        @Part("signatureSvg") signatureSvg: RequestBody?,
        @Part("signedByName") signedByName: RequestBody?,
        @Part("deliveredWithoutPayment") deliveredWithoutPayment: RequestBody? = null,
        @Part("unpaidReason") unpaidReason: RequestBody? = null,
        @Part photos: List<MultipartBody.Part>?
    ): Response<SimpleMessageResponse>

    /** Marcar entrega como fallida (multipart con fotos) */
    @Multipart
    @POST("driver/{token}/fail/{deliveryId}")
    suspend fun markFailed(
        @Path("token") driverToken: String,
        @Path("deliveryId") deliveryId: Int,
        @Header("X-Idempotency-Key") idempotencyKey: String,
        @Part("reason") reason: RequestBody,
        @Part("notes") notes: RequestBody?,
        @Part photos: List<MultipartBody.Part>?
    ): Response<SimpleMessageResponse>

    // ─── REORDENAR ───

    /** Reordenar entregas */
    @PUT("driver/{token}/reorder")
    suspend fun reorderDeliveries(
        @Path("token") driverToken: String,
        @Body orderedDeliveryIds: List<Int>
    ): Response<SimpleMessageResponse>

    // ─── CHAT CON ADMIN ───

    @GET("driver/{token}/chat")
    suspend fun getAdminChat(
        @Path("token") driverToken: String
    ): Response<List<ChatMessageDto>>

    @POST("driver/{token}/chat")
    suspend fun sendAdminMessage(
        @Path("token") driverToken: String,
        @Body request: SendMessageRequest
    ): Response<ChatMessageDto>

    // ─── CHAT CON CLIENTA ───

    @GET("driver/{token}/deliver/{deliveryId}/chat")
    suspend fun getClientChat(
        @Path("token") driverToken: String,
        @Path("deliveryId") deliveryId: Int
    ): Response<List<ChatMessageDto>>

    @POST("driver/{token}/deliver/{deliveryId}/chat")
    suspend fun sendClientMessage(
        @Path("token") driverToken: String,
        @Path("deliveryId") deliveryId: Int,
        @Body request: SendMessageRequest
    ): Response<ChatMessageDto>

    // ─── TANDAS EN RUTA Y CORTE DE CAJA ───

    @POST("driver/{token}/tanda-payment")
    suspend fun recordTandaPayment(
        @Path("token") driverToken: String,
        @Body request: DriverTandaPaymentRequest
    ): Response<SimpleMessageResponse>

    @POST("driver/{token}/settle")
    suspend fun settleRoute(
        @Path("token") driverToken: String,
        @Body request: RouteSettlementRequest
    ): Response<RouteSettlementDto>

    @GET("driver/{token}/settlement")
    suspend fun getSettlement(
        @Path("token") driverToken: String
    ): Response<RouteSettlementDto>

    // ─── GASTOS ───

    @Multipart
    @POST("driver/{token}/expenses")
    suspend fun addExpense(
        @Path("token") driverToken: String,
        @Part("amount") amount: RequestBody,
        @Part("expenseType") expenseType: RequestBody,
        @Part("notes") notes: RequestBody?,
        @Part photo: MultipartBody.Part?
    ): Response<Unit>

    // ─── C.A.M.I. COPILOT ───

    @POST("driver/{token}/cami-command")
    suspend fun camiCommand(
        @Path("token") driverToken: String,
        @Body request: DriverCamiRequest
    ): Response<DriverCamiResponse>

    /** Briefing de ruta al iniciar (Feature #6) */
    @GET("cami/route-briefing/{routeId}")
    suspend fun getRouteBriefing(
        @Path("routeId") routeId: Int
    ): Response<RouteBriefingResponse>

    // ─── PAQUETES QR ───

    /** Escanear bolsa al cargar camioneta (action="Load") o al entregar (action="Deliver") */
    @POST("driver/{token}/packages/scan")
    suspend fun scanPackage(
        @Path("token") driverToken: String,
        @Body request: ScanPackageRequest
    ): Response<ScanPackageResponse>

    // ─── FCM — PUSH NOTIFICATIONS ───

    @POST("push/subscribe-fcm")
    suspend fun subscribeFcm(@Body request: FcmSubscribeRequest): Response<Unit>

    @DELETE("push/unsubscribe-fcm")
    suspend fun unsubscribeFcm(@Query("fcmToken") fcmToken: String): Response<Unit>
}
