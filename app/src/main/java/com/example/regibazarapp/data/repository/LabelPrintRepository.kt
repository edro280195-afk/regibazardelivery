package com.example.regibazarapp.data.repository

import com.example.regibazarapp.data.models.CreateLabelPrintEventDto
import com.example.regibazarapp.data.models.LabelAssetDto
import com.example.regibazarapp.data.models.LabelPrintContextDto
import com.example.regibazarapp.data.models.LabelTargetKind
import com.example.regibazarapp.data.models.LabelTemplateSummaryDto
import com.example.regibazarapp.data.network.AdminApi
import com.example.regibazarapp.data.network.NetworkModule
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import retrofit2.Response

private data class LabelPrintApiError(
    @SerializedName("message") val message: String?,
    @SerializedName("detail") val detail: String?
)

/**
 * Sólo consume versiones publicadas desde la API. Android nunca interpreta un
 * borrador ni guarda una copia local del diseño, para conservar la misma salida
 * que la web en cada reimpresión.
 */
class LabelPrintRepository(
    private val api: AdminApi = NetworkModule.adminApi,
    private val gson: Gson = Gson()
) {
    suspend fun getDefaultTemplate(kind: LabelTargetKind): RepositoryResult<LabelTemplateSummaryDto> {
        return when (val result = request { api.getLabelTemplates() }) {
            is RepositoryResult.Success -> {
                val template = result.value.firstOrNull {
                    it.kind == kind.apiName && it.isDefault && !it.isArchived && it.publishedVersionId != null
                }
                if (template == null) {
                    RepositoryResult.Error("No hay una plantilla publicada y predeterminada para esta etiqueta.")
                } else {
                    RepositoryResult.Success(template)
                }
            }
            is RepositoryResult.Error -> result
        }
    }

    suspend fun getAssets(): RepositoryResult<List<LabelAssetDto>> = request { api.getLabelAssets() }

    suspend fun getPrintContext(
        kind: LabelTargetKind,
        templateId: String,
        targetId: String
    ): RepositoryResult<LabelPrintContextDto> = when (kind) {
        LabelTargetKind.InventoryBox -> request { api.getBoxLabelPrintContext(templateId, targetId) }
        LabelTargetKind.InventoryItem -> request { api.getItemLabelPrintContext(templateId, targetId) }
        LabelTargetKind.OrderPackage -> request { api.getPackageLabelPrintContext(templateId, targetId) }
    }

    suspend fun registerPrint(request: CreateLabelPrintEventDto): RepositoryResult<Unit> =
        requestUnit { api.createLabelPrintEvent(request) }

    private suspend fun <T> request(call: suspend () -> Response<T>): RepositoryResult<T> = try {
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

    private suspend fun requestUnit(call: suspend () -> Response<*>): RepositoryResult<Unit> = try {
        val response = call()
        if (response.isSuccessful) RepositoryResult.Success(Unit)
        else RepositoryResult.Error(parseError(response), response.code())
    } catch (exception: Exception) {
        RepositoryResult.Error(exception.localizedMessage ?: "No se pudo registrar la impresión.")
    }

    private fun parseError(response: Response<*>): String {
        val raw = response.errorBody()?.string()
        if (raw.isNullOrBlank()) {
            return when (response.code()) {
                401 -> "Tu sesión expiró. Inicia sesión de nuevo."
                404 -> "No encontramos la plantilla o el elemento a imprimir."
                409 -> "La plantilla cambió. Vuelve a abrir la etiqueta."
                else -> "El servidor respondió con error ${response.code()}."
            }
        }
        return runCatching { gson.fromJson(raw, LabelPrintApiError::class.java) }
            .getOrNull()
            ?.let { it.message?.takeIf(String::isNotBlank) ?: it.detail?.takeIf(String::isNotBlank) }
            ?: raw.trim('"')
    }
}
