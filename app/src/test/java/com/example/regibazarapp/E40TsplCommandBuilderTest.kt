package com.example.regibazarapp

import com.example.regibazarapp.labelprint.E40TsplCommandBuilder
import com.example.regibazarapp.labelprint.MonochromeRaster
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class E40TsplCommandBuilderTest {
    @Test
    fun build_createsE40SizedTsplBitmapWithPackedBlackPixels() {
        val raster = MonochromeRaster(
            width = 2,
            height = 1,
            blackPixels = booleanArrayOf(true, false)
        )

        val command = E40TsplCommandBuilder.build(raster)
        val header = "SIZE 100 mm,150 mm\r\n" +
            "GAP 3 mm,0 mm\r\n" +
            "DIRECTION 1\r\n" +
            "REFERENCE 0,0\r\n" +
            "CLS\r\n" +
            "BITMAP 0,0,100,1200,0,"
        val headerSize = header.encodeToByteArray().size
        val footer = "\r\nPRINT 1,1\r\n".encodeToByteArray()

        assertEquals(headerSize + 100 * 1200 + footer.size, command.size)
        assertEquals(0xFF, command[headerSize].toInt() and 0xFF)
        assertEquals(0x00, command[headerSize + 50].toInt() and 0xFF)
        assertTrue(command.copyOfRange(command.size - footer.size, command.size).contentEquals(footer))
    }

    @Test
    fun scaleTo_keepsNearestPixelsInTheExpectedQuadrants() {
        val source = MonochromeRaster(
            width = 2,
            height = 2,
            blackPixels = booleanArrayOf(true, false, false, true)
        )

        val scaled = source.scaleTo(4, 4)

        assertTrue(scaled.isBlack(0, 0))
        assertTrue(scaled.isBlack(1, 1))
        assertTrue(!scaled.isBlack(2, 0))
        assertTrue(!scaled.isBlack(0, 2))
        assertTrue(scaled.isBlack(3, 3))
    }
}
