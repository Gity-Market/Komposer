package ir.gity.komposer.core.widget.factory

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import ir.gity.komposer.core.KomposerRenderException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KomposerColorTest {

    @Test
    fun sixDigitImpliesFullAlpha() {
        val color = parseKomposerColor("#6200EE")
        assertEquals(0xFF6200EE.toInt(), color.toArgb())
    }

    @Test
    fun eightDigitCarriesAlpha() {
        val color = parseKomposerColor("#806200EE")
        assertEquals(0x806200EE.toInt(), color.toArgb())
    }

    @Test
    fun caseInsensitive() {
        assertEquals(parseKomposerColor("#ff6200ee"), parseKomposerColor("#FF6200EE"))
    }

    @Test
    fun whiteOpaque() {
        assertEquals(Color.White, parseKomposerColor("#FFFFFF"))
    }

    @Test
    fun invalidHexFails() {
        assertFailsWith<KomposerRenderException> { parseKomposerColor("#zzzzzz") }
    }
}
