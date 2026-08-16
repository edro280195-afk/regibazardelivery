package com.example.regibazarapp.data.local

import android.content.Context

/**
 * ══════════════════════════════════════════════════════════════
 *  WidgetDataStore — Puente entre RouteViewModel y el Widget
 *
 *  SharedPreferences cross-process: el widget (proceso diferente)
 *  puede leer los datos que el ViewModel escribió.
 * ══════════════════════════════════════════════════════════════
 */
class WidgetDataStore(context: Context) {

    private val prefs = context.getSharedPreferences("regibazar_widget", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NAME = "next_name"
        private const val KEY_ADDRESS = "next_address"
        private const val KEY_BALANCE = "next_balance"
        private const val KEY_DELIVERED = "delivered"
        private const val KEY_TOTAL = "total"
        private const val KEY_IS_COMPLETE = "is_complete"
    }

    fun save(
        name: String,
        address: String?,
        balance: Double,
        delivered: Int,
        total: Int,
        isComplete: Boolean = false
    ) {
        prefs.edit()
            .putString(KEY_NAME, name)
            .putString(KEY_ADDRESS, address ?: "")
            .putFloat(KEY_BALANCE, balance.toFloat())
            .putInt(KEY_DELIVERED, delivered)
            .putInt(KEY_TOTAL, total)
            .putBoolean(KEY_IS_COMPLETE, isComplete)
            .apply()
    }

    fun getNextName(): String = prefs.getString(KEY_NAME, "") ?: ""
    fun getNextAddress(): String = prefs.getString(KEY_ADDRESS, "") ?: ""
    fun getBalance(): Double = prefs.getFloat(KEY_BALANCE, 0f).toDouble()
    fun getDelivered(): Int = prefs.getInt(KEY_DELIVERED, 0)
    fun getTotal(): Int = prefs.getInt(KEY_TOTAL, 0)
    fun isComplete(): Boolean = prefs.getBoolean(KEY_IS_COMPLETE, false)
}
