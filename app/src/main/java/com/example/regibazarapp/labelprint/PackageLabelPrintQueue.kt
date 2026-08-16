package com.example.regibazarapp.labelprint

/** Estado visible de cada bolsa dentro de una impresión por lote. */
enum class PackageLabelPrintStatus {
    PENDING,
    PREPARING,
    SENDING,
    SENT,
    FAILED
}

data class PackageLabelPrintQueueItem(
    val packageId: String,
    val packageNumber: Int,
    val clientName: String,
    val status: PackageLabelPrintStatus = PackageLabelPrintStatus.PENDING,
    val detail: String? = null
)

data class PackageLabelPrintQueue(
    val items: List<PackageLabelPrintQueueItem>
) {
    val sentCount: Int
        get() = items.count { it.status == PackageLabelPrintStatus.SENT }

    val failedCount: Int
        get() = items.count { it.status == PackageLabelPrintStatus.FAILED }

    val activeCount: Int
        get() = items.count { it.status == PackageLabelPrintStatus.PREPARING || it.status == PackageLabelPrintStatus.SENDING }

    fun update(
        packageId: String,
        status: PackageLabelPrintStatus,
        detail: String? = null
    ): PackageLabelPrintQueue = copy(
        items = items.map { item ->
            if (item.packageId == packageId) item.copy(status = status, detail = detail) else item
        }
    )
}
