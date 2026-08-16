package com.example.regibazarapp.data.models

import com.google.gson.annotations.SerializedName

/** Contrato estable compartido por el diseñador web y los renderizadores móviles. */
data class LabelTemplateSummaryDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("kind") val kind: String,
    @SerializedName("printerProfile") val printerProfile: String,
    @SerializedName("isDefault") val isDefault: Boolean,
    @SerializedName("isArchived") val isArchived: Boolean,
    @SerializedName("publishedVersionId") val publishedVersionId: String?,
    @SerializedName("publishedVersionNumber") val publishedVersionNumber: Int?,
    @SerializedName("updatedAt") val updatedAt: String
)

data class LabelAssetDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("url") val url: String,
    @SerializedName("isArchived") val isArchived: Boolean
)

data class PublishedLabelTemplateDto(
    @SerializedName("templateId") val templateId: String,
    @SerializedName("templateName") val templateName: String,
    @SerializedName("kind") val kind: String,
    @SerializedName("printerProfile") val printerProfile: String,
    @SerializedName("versionId") val versionId: String,
    @SerializedName("versionNumber") val versionNumber: Int,
    @SerializedName("designJson") val designJson: String
)

data class LabelPrintContextDto(
    @SerializedName("template") val template: PublishedLabelTemplateDto,
    @SerializedName("targetKind") val targetKind: String,
    @SerializedName("targetId") val targetId: String,
    @SerializedName("data") val data: Map<String, String>
)

data class CreateLabelPrintEventDto(
    @SerializedName("labelTemplateVersionId") val labelTemplateVersionId: String,
    @SerializedName("targetKind") val targetKind: Int,
    @SerializedName("targetId") val targetId: String,
    @SerializedName("printerProfile") val printerProfile: Int,
    @SerializedName("method") val method: Int,
    @SerializedName("copies") val copies: Int = 1
)

enum class LabelTargetKind(val apiValue: Int, val apiName: String) {
    InventoryBox(0, "InventoryBox"),
    InventoryItem(1, "InventoryItem"),
    OrderPackage(2, "OrderPackage")
}

enum class LabelPrinterProfile(
    val apiValue: Int,
    val apiName: String,
    val widthMm: Float,
    val heightMm: Float,
    val displayName: String
) {
    NiimbotB1_50x50(0, "NiimbotB1_50x50", 50f, 50f, "NIIMBOT B1 · 50 × 50 mm"),
    AiyinE40_4x6(1, "AiyinE40_4x6", 101.6f, 152.4f, "AIYIN E40 Pro · 4 × 6 in");

    companion object {
        fun fromApi(value: String): LabelPrinterProfile? = entries.firstOrNull { it.apiName == value }
    }
}

enum class LabelPrintMethod(val apiValue: Int) {
    NativeBluetooth(2),
    SystemShare(3),
    NativeSystemPrint(6)
}
