package com.example.regibazarapp.labelprint

import android.graphics.Bitmap
import com.example.regibazarapp.data.models.CreateLabelPrintEventDto
import com.example.regibazarapp.data.models.LabelPrintContextDto
import com.example.regibazarapp.data.models.LabelPrintMethod
import com.example.regibazarapp.data.models.LabelPrinterProfile
import com.example.regibazarapp.data.models.LabelTargetKind
import com.example.regibazarapp.data.repository.LabelPrintRepository
import com.example.regibazarapp.data.repository.RepositoryResult

data class RenderedLabelPreview(
    val bitmap: Bitmap,
    val context: LabelPrintContextDto,
    val profile: LabelPrinterProfile,
    val title: String
)

/** Caso de uso común para caja, artículo y bolsa. */
class LabelPrintCoordinator(
    private val repository: LabelPrintRepository = LabelPrintRepository(),
    private val renderer: ThermalLabelRenderer = ThermalLabelRenderer()
) {
    suspend fun prepare(
        kind: LabelTargetKind,
        targetId: String,
        title: String
    ): RepositoryResult<RenderedLabelPreview> {
        val template = when (val result = repository.getDefaultTemplate(kind)) {
            is RepositoryResult.Success -> result.value
            is RepositoryResult.Error -> return result
        }
        val context = when (val result = repository.getPrintContext(kind, template.id, targetId)) {
            is RepositoryResult.Success -> result.value
            is RepositoryResult.Error -> return result
        }
        val profile = LabelPrinterProfile.fromApi(context.template.printerProfile)
            ?: return RepositoryResult.Error("La impresora de esta plantilla no es compatible con Android todavía.")
        val assets = when (val result = repository.getAssets()) {
            is RepositoryResult.Success -> result.value.filterNot { it.isArchived }.associate { it.id to it.url }
            is RepositoryResult.Error -> return RepositoryResult.Error("No pudimos obtener las imágenes de la etiqueta: ${result.message}")
        }
        return runCatching {
            RenderedLabelPreview(
                bitmap = renderer.render(context.template.designJson, profile, context.data, assets),
                context = context,
                profile = profile,
                title = title
            )
        }.fold(
            onSuccess = { value -> RepositoryResult.Success(value) },
            onFailure = { RepositoryResult.Error(it.message ?: "No pudimos generar la etiqueta térmica.") }
        )
    }

    suspend fun register(preview: RenderedLabelPreview, method: LabelPrintMethod): RepositoryResult<Unit> {
        val targetKind = LabelTargetKind.entries.firstOrNull { it.apiName == preview.context.targetKind }
            ?: return RepositoryResult.Error("La etiqueta no tiene un tipo de destino válido.")
        return repository.registerPrint(
            CreateLabelPrintEventDto(
                labelTemplateVersionId = preview.context.template.versionId,
                targetKind = targetKind.apiValue,
                targetId = preview.context.targetId,
                printerProfile = preview.profile.apiValue,
                method = method.apiValue,
                copies = 1
            )
        )
    }
}
