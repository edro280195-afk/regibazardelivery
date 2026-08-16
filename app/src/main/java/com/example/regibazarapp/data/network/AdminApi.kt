package com.example.regibazarapp.data.network

import com.example.regibazarapp.data.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * ══════════════════════════════════════════════════════
 *  Admin API Interface – Endpoints con autenticación JWT
 *  Base URL: https://regibazarapi.onrender.com/api/
 * ══════════════════════════════════════════════════════
 */
interface AdminApi {

    // ─── AUTH ───

    @POST("auth/login")
    suspend fun login(
        @Body request: AdminLoginRequest
    ): Response<AdminLoginResponse>

    // ─── RUTAS (requiere JWT) ───

    @GET("routes")
    suspend fun getRoutes(): Response<List<RouteListDto>>

    @GET("routes/{id}")
    suspend fun getRoute(@Path("id") routeId: Int): Response<RouteListDto>

    @POST("routes")
    suspend fun createRoute(@Body request: CreateRouteRequest): Response<CreateRouteResponse>

    @PATCH("orders/{id}/packages")
    suspend fun setPackages(
        @Path("id") orderId: Int,
        @Body request: SetPackagesRequest
    ): Response<OrderSummaryDto>

    @POST("routes/preview")
    suspend fun previewRoute(@Body request: PreviewRouteRequest): Response<PreviewRouteResponse>

    @POST("routes/ai-select")
    suspend fun selectRouteWithCami(
        @Body request: AiRouteSelectionRequest
    ): Response<AiRouteSelectionResponse>

    @GET("routes/available-tandas")
    suspend fun getAvailableTandas(): Response<List<AvailableTandaDto>>

    @PUT("routes/{id}/recompose")
    suspend fun recomposeRoute(
        @Path("id") routeId: Int,
        @Body request: RecomposeRouteRequest
    ): Response<RecomposeRouteResponse>

    @POST("routes/{id}/optimize")
    suspend fun optimizeRoute(
        @Path("id") routeId: Int,
        @Query("lat") latitude: Double? = null,
        @Query("lng") longitude: Double? = null
    ): Response<SimpleMessageResponse>

    @PUT("routes/{id}/reorder")
    suspend fun reorderRoute(
        @Path("id") routeId: Int,
        @Body deliveryIds: List<Int>
    ): Response<SimpleMessageResponse>

    @DELETE("routes/{id}/remove-order/{orderId}")
    suspend fun removeOrder(
        @Path("id") routeId: Int,
        @Path("orderId") orderId: Int
    ): Response<SimpleMessageResponse>

    @DELETE("routes/{id}/remove-tanda/{participantId}")
    suspend fun removeTanda(
        @Path("id") routeId: Int,
        @Path("participantId") participantId: String
    ): Response<SimpleMessageResponse>

    @DELETE("routes/{id}")
    suspend fun deleteRoute(@Path("id") routeId: Int): Response<SimpleMessageResponse>

    @POST("routes/{id}/liquidate")
    suspend fun liquidateRoute(@Path("id") routeId: Int): Response<SimpleMessageResponse>

    @GET("orders")
    suspend fun getOrders(): Response<List<OrderSummaryDto>>

    @GET("clients")
    suspend fun getClients(): Response<List<ClientDto>>

    @POST("clients/bulk-geocode")
    suspend fun bulkGeocodeClients(
        @Body request: BulkGeocodeRequest
    ): Response<List<BulkGeocodeResultDto>>

    @POST("clients/{id}/set-coordinates")
    suspend fun setClientCoordinates(
        @Path("id") clientId: Int,
        @Body request: SetClientCoordinatesRequest
    ): Response<Unit>

    // ─── INVENTARIO FÍSICO / CAJAS NFC ───

    @GET("inventory/boxes")
    suspend fun getInventoryBoxes(@Query("search") search: String? = null): Response<List<InventoryBoxSummaryDto>>

    @GET("inventory/boxes/{id}")
    suspend fun getInventoryBox(@Path("id") boxId: String): Response<InventoryBoxDto>

    @GET("inventory/boxes/by-token/{token}")
    suspend fun getInventoryBoxByToken(@Path("token") token: String): Response<InventoryBoxDto>

    @POST("inventory/boxes")
    suspend fun createInventoryBox(@Body request: CreateInventoryBoxDto): Response<InventoryBoxDto>

    @POST("inventory/boxes/{id}/bind-nfc")
    suspend fun bindInventoryNfc(
        @Path("id") boxId: String,
        @Body request: BindInventoryNfcDto
    ): Response<InventoryBoxDto>

    @POST("inventory/boxes/{id}/items")
    suspend fun addInventoryItem(
        @Path("id") boxId: String,
        @Body request: CreateInventoryItemDto
    ): Response<InventoryBoxDto>

    @POST("inventory/items/{id}/adjust")
    suspend fun adjustInventoryItem(
        @Path("id") itemId: String,
        @Body request: AdjustInventoryItemDto
    ): Response<InventoryBoxDto>

    @POST("inventory/transfers")
    suspend fun transferInventoryItems(@Body request: TransferInventoryItemsDto): Response<InventoryBoxDto>

    @GET("inventory/items/by-barcode/{barcode}")
    suspend fun getInventoryItemsByBarcode(
        @Path("barcode") barcode: String
    ): Response<List<InventoryBarcodeMatchDto>>

    @POST("inventory/boxes/{id}/counts")
    suspend fun completeInventoryCount(
        @Path("id") boxId: String,
        @Body request: CompleteInventoryCountDto
    ): Response<InventoryBoxDto>

    // ─── IMPRESIÓN DE ETIQUETAS (plantillas publicadas) ───

    @GET("label-templates")
    suspend fun getLabelTemplates(): Response<List<LabelTemplateSummaryDto>>

    @GET("label-templates/assets")
    suspend fun getLabelAssets(): Response<List<LabelAssetDto>>

    @GET("label-print-jobs/{templateId}/boxes/{boxId}")
    suspend fun getBoxLabelPrintContext(
        @Path("templateId") templateId: String,
        @Path("boxId") boxId: String
    ): Response<LabelPrintContextDto>

    @GET("label-print-jobs/{templateId}/items/{itemId}")
    suspend fun getItemLabelPrintContext(
        @Path("templateId") templateId: String,
        @Path("itemId") itemId: String
    ): Response<LabelPrintContextDto>

    @GET("label-print-jobs/{templateId}/packages/{packageId}")
    suspend fun getPackageLabelPrintContext(
        @Path("templateId") templateId: String,
        @Path("packageId") packageId: String
    ): Response<LabelPrintContextDto>

    @POST("label-print-jobs/events")
    suspend fun createLabelPrintEvent(@Body request: CreateLabelPrintEventDto): Response<Unit>

    // ─── PRODUCTOS FRECUENTES (AllowAnonymous) ───

    @GET("orders/common-products")
    suspend fun getCommonProducts(): Response<List<CommonProductDto>>

    // ─── ALERTAS C.A.M.I. (requiere JWT) ───

    @GET("cami/alerts")
    suspend fun getCamiAlerts(): Response<List<CamiAlertDto>>
}
