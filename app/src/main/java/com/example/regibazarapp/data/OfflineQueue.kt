package com.example.regibazarapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * ══════════════════════════════════════════════════════════════
 *  OfflineQueue – Persist and retry delivery/fail actions
 *                when the device has no connectivity.
 *
 *  Persistence : DataStore<Preferences> ("offline_queue_prefs")
 *  Serialisation: Gson JSON array
 * ══════════════════════════════════════════════════════════════
 */

// Separate DataStore extension — the one in TokenManager.kt is private
// and therefore cannot be reused from this file.
private val Context.offlineQueueDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "offline_queue_prefs")

// ── Pending action model ──────────────────────────────────────

data class PendingAction(
    val id: String,               // UUID
    val version: Int = 2,
    val type: String,             // "DELIVER" | "FAIL"
    val driverToken: String,
    val deliveryId: Int,
    val reason: String?,          // for FAIL
    val notes: String? = null,
    val paymentsJson: String?,    // for DELIVER (Serialized JSON)
    val evidencePaths: List<String>? = null,
    val signatureSvgPath: String? = null,
    val signedByName: String? = null,
    val timestamp: Long
) {
    val allEvidencePaths: List<String>
        get() = evidencePaths.orEmpty() + listOfNotNull(signatureSvgPath)
}

// ── Queue class ───────────────────────────────────────────────

class OfflineQueue(private val context: Context) {

    companion object {
        private val PENDING_ACTIONS_KEY = stringPreferencesKey("pending_actions")
    }

    private val gson = Gson()
    private val listType = object : TypeToken<List<PendingAction>>() {}.type

    // ── Public suspend functions ──────────────────────────────

    /** Add a new action to the tail of the queue. */
    suspend fun enqueue(action: PendingAction) {
        context.offlineQueueDataStore.edit { prefs ->
            val current = readList(prefs)
            val updated = current + action
            prefs[PENDING_ACTIONS_KEY] = gson.toJson(updated)
        }
    }

    /** Return all queued actions in insertion order. */
    suspend fun getAll(): List<PendingAction> {
        var result: List<PendingAction> = emptyList()
        context.offlineQueueDataStore.edit { prefs ->
            result = readList(prefs)
        }
        return result
    }

    /** Remove a single action by its UUID. */
    suspend fun remove(id: String) {
        context.offlineQueueDataStore.edit { prefs ->
            val current = readList(prefs)
            val updated = current.filter { it.id != id }
            prefs[PENDING_ACTIONS_KEY] = gson.toJson(updated)
        }
    }

    /** Clear the entire queue. */
    suspend fun clear() {
        context.offlineQueueDataStore.edit { prefs ->
            prefs[PENDING_ACTIONS_KEY] = "[]"
        }
    }

    // ── Reactive property ─────────────────────────────────────

    /** Emits true whenever there is at least one pending action. */
    val hasPending: Flow<Boolean> = context.offlineQueueDataStore.data.map { prefs ->
        readList(prefs).isNotEmpty()
    }

    // ── Private helpers ───────────────────────────────────────

    private fun readList(prefs: Preferences): List<PendingAction> {
        val json = prefs[PENDING_ACTIONS_KEY] ?: return emptyList()
        return try {
            gson.fromJson(json, listType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
