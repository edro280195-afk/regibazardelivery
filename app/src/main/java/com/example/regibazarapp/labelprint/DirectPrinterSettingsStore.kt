package com.example.regibazarapp.labelprint

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.directPrinterDataStore: DataStore<Preferences> by preferencesDataStore(name = "regi_bazar_direct_printer")

enum class PrinterScope {
    PACKAGE, // Etiquetas de bolsas / pedidos
    BOX      // Etiquetas de cajas / bodega
}

/** Configuración local de la impresora elegida por administración según su módulo. */
data class DirectPrinterSettings(
    val address: String? = null,
    val deviceName: String? = null
) {
    val isConfigured: Boolean
        get() = !address.isNullOrBlank()
}

class DirectPrinterSettingsStore(
    private val context: Context,
    private val scope: PrinterScope = PrinterScope.PACKAGE
) {
    private companion object {
        val LEGACY_PACKAGE_ADDRESS_KEY = stringPreferencesKey("aiyin_e40_address")
        val LEGACY_PACKAGE_NAME_KEY = stringPreferencesKey("aiyin_e40_name")

        val PACKAGE_ADDRESS_KEY = stringPreferencesKey("package_printer_address")
        val PACKAGE_NAME_KEY = stringPreferencesKey("package_printer_name")

        val BOX_ADDRESS_KEY = stringPreferencesKey("box_printer_address")
        val BOX_NAME_KEY = stringPreferencesKey("box_printer_name")
    }

    private val addressKey = when (scope) {
        PrinterScope.PACKAGE -> PACKAGE_ADDRESS_KEY
        PrinterScope.BOX -> BOX_ADDRESS_KEY
    }

    private val nameKey = when (scope) {
        PrinterScope.PACKAGE -> PACKAGE_NAME_KEY
        PrinterScope.BOX -> BOX_NAME_KEY
    }

    val settings: Flow<DirectPrinterSettings> = context.directPrinterDataStore.data.map { preferences ->
        val addr = preferences[addressKey]
            ?: if (scope == PrinterScope.PACKAGE) preferences[LEGACY_PACKAGE_ADDRESS_KEY] else null
        val name = preferences[nameKey]
            ?: if (scope == PrinterScope.PACKAGE) preferences[LEGACY_PACKAGE_NAME_KEY] else null

        DirectPrinterSettings(
            address = addr,
            deviceName = name
        )
    }

    suspend fun save(printer: PairedBluetoothPrinter) {
        context.directPrinterDataStore.edit { preferences ->
            preferences[addressKey] = printer.address
            preferences[nameKey] = printer.name
        }
    }

    suspend fun clear() {
        context.directPrinterDataStore.edit { preferences ->
            preferences.remove(addressKey)
            preferences.remove(nameKey)
            if (scope == PrinterScope.PACKAGE) {
                preferences.remove(LEGACY_PACKAGE_ADDRESS_KEY)
                preferences.remove(LEGACY_PACKAGE_NAME_KEY)
            }
        }
    }
}
