package com.example.regibazarapp.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface NfcWriteResult {
    data object Success : NfcWriteResult
    data class Error(val message: String) : NfcWriteResult
}

/** Lector/escritor NDEF para las etiquetas de cajas NTAG215. */
class NfcInventoryReader(private val activity: FragmentActivity) {
    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    fun start(onTagDiscovered: (Tag) -> Unit): Boolean {
        val currentAdapter = adapter ?: return false
        if (!currentAdapter.isEnabled) return false

        currentAdapter.enableReaderMode(
            activity,
            { tag -> activity.runOnUiThread { onTagDiscovered(tag) } },
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B,
            null
        )
        return true
    }

    fun stop() {
        adapter?.disableReaderMode(activity)
    }

    companion object {
        suspend fun readUrl(tag: Tag): String? = withContext(Dispatchers.IO) {
            val ndef = Ndef.get(tag) ?: return@withContext null
            try {
                ndef.connect()
                ndef.ndefMessage?.records
                    ?.firstNotNullOfOrNull { record -> record.toUri()?.toString() }
            } catch (_: Exception) {
                null
            } finally {
                runCatching { ndef.close() }
            }
        }

        suspend fun writeUrl(tag: Tag, url: String): NfcWriteResult = withContext(Dispatchers.IO) {
            val message = NdefMessage(arrayOf(NdefRecord.createUri(url)))
            val ndef = Ndef.get(tag)
            if (ndef == null) {
                val formatable = NdefFormatable.get(tag)
                    ?: return@withContext NfcWriteResult.Error("Esta etiqueta no soporta formato NDEF.")
                try {
                    formatable.connect()
                    formatable.format(message)
                } catch (_: Exception) {
                    return@withContext NfcWriteResult.Error("No pudimos preparar esta etiqueta para NFC.")
                } finally {
                    runCatching { formatable.close() }
                }

                return@withContext if (readUrl(tag) == url) {
                    NfcWriteResult.Success
                } else {
                    NfcWriteResult.Error("La etiqueta se preparó, pero no pasó la verificación. Intenta de nuevo.")
                }
            }
            try {
                ndef.connect()
                if (!ndef.isWritable) return@withContext NfcWriteResult.Error("La etiqueta está bloqueada y no se puede escribir.")
                if (message.toByteArray().size > ndef.maxSize) return@withContext NfcWriteResult.Error("La etiqueta no tiene espacio suficiente.")
                ndef.writeNdefMessage(message)
                val verifiedUrl = ndef.ndefMessage?.records
                    ?.firstNotNullOfOrNull { record -> record.toUri()?.toString() }
                if (verifiedUrl == url) NfcWriteResult.Success
                else NfcWriteResult.Error("La etiqueta se escribió, pero no pasó la verificación. Intenta de nuevo.")
            } catch (_: Exception) {
                NfcWriteResult.Error("No pudimos escribir la etiqueta. Intenta mantener el teléfono quieto.")
            } finally {
                runCatching { ndef.close() }
            }
        }

        fun tagUid(tag: Tag): String = tag.id.joinToString(separator = "") { byte -> "%02X".format(byte) }

        fun tokenFromInventoryUrl(url: String): String? {
            val marker = "/caja/"
            val markerPosition = url.indexOf(marker)
            if (markerPosition < 0) return null
            return url.substring(markerPosition + marker.length)
                .substringBefore('?')
                .substringBefore('#')
                .takeIf(String::isNotBlank)
        }
    }
}
