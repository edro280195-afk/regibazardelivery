package com.example.regibazarapp.service

import android.content.Context
import androidx.work.*
import com.example.regibazarapp.data.DeliveryEvidenceStore
import com.example.regibazarapp.data.OfflineQueue
import com.example.regibazarapp.data.TokenManager
import com.example.regibazarapp.data.models.PaymentInputDto
import com.example.regibazarapp.data.network.NetworkModule
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * ══════════════════════════════════════════════════════════════
 *  SyncWorker — WorkManager Background Sync
 *
 *  Se dispara automáticamente cuando el dispositivo recupera
 *  la conexión a internet. Procesa la cola offline (DELIVER/FAIL)
 *  incluso si la app está en background o fue cerrada.
 * ══════════════════════════════════════════════════════════════
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_DRIVER_TOKEN = "driver_token"
        private const val WORK_NAME = "regibazar_offline_sync"

        /** Encola un único trabajo que se ejecuta en cuanto haya red. */
        fun schedule(context: Context, driverToken: String) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setInputData(workDataOf(KEY_DRIVER_TOKEN to driverToken))
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }

    override suspend fun doWork(): Result {
        val driverToken = inputData.getString(KEY_DRIVER_TOKEN)
            ?: return Result.failure()

        // Restaurar JWT para las llamadas de red
        val tokenManager = TokenManager(applicationContext)
        val jwt = tokenManager.getJwtTokenSync()
        if (jwt != null) NetworkModule.jwtToken = jwt

        val offlineQueue = OfflineQueue(applicationContext)
        val evidenceStore = DeliveryEvidenceStore(applicationContext)
        val pending = withContext(Dispatchers.IO) { offlineQueue.getAll() }

        if (pending.isEmpty()) return Result.success()

        var allSucceeded = true

        pending.forEach { action ->
            try {
                val actionSucceeded = when (action.type) {
                    "DELIVER" -> {
                        val paymentsJson = action.paymentsJson ?: "[]"
                        val textMediaType = "text/plain".toMediaTypeOrNull()
                        val signatureSvg = evidenceStore.readText(action.signatureSvgPath)
                        val evidenceParts = evidenceStore.buildPhotoParts(action.evidencePaths.orEmpty())

                        val response = NetworkModule.api.markDelivered(
                            driverToken = action.driverToken,
                            deliveryId = action.deliveryId,
                            idempotencyKey = "sync-${action.id}",
                            notes = (action.notes ?: "Entregado offline (WorkManager sync)").toRequestBody(textMediaType),
                            payments = paymentsJson.toRequestBody(textMediaType),
                            paymentsJson = paymentsJson.toRequestBody(textMediaType),
                            signatureSvg = signatureSvg?.toRequestBody(textMediaType),
                            signedByName = action.signedByName?.toRequestBody(textMediaType),
                            photos = evidenceParts.ifEmpty { null }
                        )
                        when {
                            response.isSuccessful -> true
                            response.code().isRetriableSyncStatus() -> false
                            else -> true
                        }
                    }
                    "FAIL" -> {
                        val textMediaType = "text/plain".toMediaTypeOrNull()
                        val evidenceParts = evidenceStore.buildPhotoParts(action.evidencePaths.orEmpty())

                        val response = NetworkModule.api.markFailed(
                            driverToken = action.driverToken,
                            deliveryId = action.deliveryId,
                            idempotencyKey = "sync-${action.id}",
                            reason = (action.reason ?: "Sin conexión").toRequestBody("text/plain".toMediaTypeOrNull()),
                            notes = (action.notes ?: "Reportado offline (WorkManager sync)").toRequestBody(textMediaType),
                            photos = evidenceParts.ifEmpty { null }
                        )
                        when {
                            response.isSuccessful -> true
                            response.code().isRetriableSyncStatus() -> false
                            else -> true
                        }
                    }
                    else -> false
                }
                if (actionSucceeded) {
                    offlineQueue.remove(action.id)
                    evidenceStore.delete(action.allEvidencePaths)
                } else {
                    allSucceeded = false
                }
            } catch (e: Exception) {
                allSucceeded = false
            }
        }

        return if (allSucceeded) Result.success() else Result.retry()
    }

    private fun Int.isRetriableSyncStatus(): Boolean =
        this == 408 || this == 429 || this in 500..599
}
