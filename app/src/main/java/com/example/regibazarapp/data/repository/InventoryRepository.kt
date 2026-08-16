package com.example.regibazarapp.data.repository

import com.example.regibazarapp.data.models.AdjustInventoryItemDto
import com.example.regibazarapp.data.models.BindInventoryNfcDto
import com.example.regibazarapp.data.models.CreateInventoryBoxDto
import com.example.regibazarapp.data.models.CreateInventoryItemDto
import com.example.regibazarapp.data.models.CompleteInventoryCountDto
import com.example.regibazarapp.data.models.InventoryBarcodeMatchDto
import com.example.regibazarapp.data.models.InventoryBoxDto
import com.example.regibazarapp.data.models.InventoryBoxSummaryDto
import com.example.regibazarapp.data.models.TransferInventoryItemsDto
import com.example.regibazarapp.data.network.AdminApi
import com.example.regibazarapp.data.network.NetworkModule
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import retrofit2.Response

private data class InventoryApiError(
    @SerializedName("message") val message: String?,
    @SerializedName("detail") val detail: String?
)

class InventoryRepository(
    private val api: AdminApi = NetworkModule.adminApi,
    private val gson: Gson = Gson()
) {
    suspend fun getBoxes(search: String? = null): RepositoryResult<List<InventoryBoxSummaryDto>> =
        request { api.getInventoryBoxes(search) }

    suspend fun getBox(boxId: String): RepositoryResult<InventoryBoxDto> =
        request { api.getInventoryBox(boxId) }

    suspend fun getBoxByToken(token: String): RepositoryResult<InventoryBoxDto> =
        request { api.getInventoryBoxByToken(token) }

    suspend fun createBox(request: CreateInventoryBoxDto): RepositoryResult<InventoryBoxDto> =
        request { api.createInventoryBox(request) }

    suspend fun bindNfc(boxId: String, tagUid: String): RepositoryResult<InventoryBoxDto> =
        request { api.bindInventoryNfc(boxId, BindInventoryNfcDto(tagUid)) }

    suspend fun addItem(boxId: String, request: CreateInventoryItemDto): RepositoryResult<InventoryBoxDto> =
        request { api.addInventoryItem(boxId, request) }

    suspend fun adjustItem(itemId: String, request: AdjustInventoryItemDto): RepositoryResult<InventoryBoxDto> =
        request { api.adjustInventoryItem(itemId, request) }

    suspend fun transfer(request: TransferInventoryItemsDto): RepositoryResult<InventoryBoxDto> =
        request { api.transferInventoryItems(request) }

    suspend fun getItemsByBarcode(barcode: String): RepositoryResult<List<InventoryBarcodeMatchDto>> =
        request { api.getInventoryItemsByBarcode(barcode) }

    suspend fun completeCount(boxId: String, request: CompleteInventoryCountDto): RepositoryResult<InventoryBoxDto> =
        request { api.completeInventoryCount(boxId, request) }

    private suspend fun <T> request(call: suspend () -> Response<T>): RepositoryResult<T> =
        try {
            val response = call()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                RepositoryResult.Success(body)
            } else {
                RepositoryResult.Error(parseError(response), response.code())
            }
        } catch (exception: Exception) {
            RepositoryResult.Error(exception.localizedMessage ?: "No se pudo conectar con Regi Bazar.")
        }

    private fun parseError(response: Response<*>): String {
        val raw = response.errorBody()?.string()
        if (raw.isNullOrBlank()) {
            return when (response.code()) {
                401 -> "Tu sesión expiró o no se ha iniciado. Inicia sesión como Administrador."
                403 -> "Acceso denegado (403). Tu cuenta requiere permisos de Administrador para gestionar la bodega."
                404 -> "No encontramos esa caja o artículo."
                else -> "El servidor respondió con error ${response.code()}."
            }
        }

        val parsed = runCatching { gson.fromJson(raw, InventoryApiError::class.java) }
            .getOrNull()
            ?.let { it.message?.takeIf(String::isNotBlank) ?: it.detail?.takeIf(String::isNotBlank) }
            ?: raw.trim('"')

        return when (response.code()) {
            403 -> "Acceso denegado (403): $parsed"
            401 -> "Sesión no válida (401): $parsed"
            else -> parsed
        }
    }
}
