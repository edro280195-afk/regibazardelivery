package com.example.regibazarapp

import com.example.regibazarapp.labelprint.PackageLabelPrintQueue
import com.example.regibazarapp.labelprint.PackageLabelPrintQueueItem
import com.example.regibazarapp.labelprint.PackageLabelPrintStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class PackageLabelPrintQueueTest {
    @Test
    fun update_tracksSentFailedAndActiveItemsWithoutChangingOthers() {
        val queue = PackageLabelPrintQueue(
            listOf(
                PackageLabelPrintQueueItem("package-a", 1, "Ana"),
                PackageLabelPrintQueueItem("package-b", 2, "Rosa"),
                PackageLabelPrintQueueItem("package-c", 1, "Luz")
            )
        )

        val updated = queue
            .update("package-a", PackageLabelPrintStatus.SENT, "Enviada")
            .update("package-b", PackageLabelPrintStatus.FAILED, "Sin conexión")
            .update("package-c", PackageLabelPrintStatus.SENDING)

        assertEquals(1, updated.sentCount)
        assertEquals(1, updated.failedCount)
        assertEquals(1, updated.activeCount)
        assertEquals(PackageLabelPrintStatus.SENDING, updated.items[2].status)
    }
}
