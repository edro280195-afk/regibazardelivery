package com.example.regibazarapp

import com.example.regibazarapp.nfc.NfcInventoryReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InventoryNfcUtilsTest {
    @Test
    fun `extrae el token de una liga de caja`() {
        assertEquals(
            "a1b2c3",
            NfcInventoryReader.tokenFromInventoryUrl("https://regibazar.com/caja/a1b2c3")
        )
    }

    @Test
    fun `ignora ligas que no son de inventario`() {
        assertNull(NfcInventoryReader.tokenFromInventoryUrl("https://regibazar.com/pedido/abc"))
    }
}
