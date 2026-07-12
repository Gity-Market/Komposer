package ir.gity.komposer.core.widget.factory

import androidx.compose.ui.graphics.Color
import ir.gity.komposer.core.KomposerRenderException

/**
 * Parses a `#RRGGBB` / `#AARRGGBB` wire color (SPEC-0002) into a Compose [Color].
 *
 * Implemented by parsing the hex digits directly rather than via
 * `android.graphics.Color.parseColor`, which would drag the Android framework
 * (Robolectric) into what is a plain JVM unit test (SPEC-0004 §3).
 */
fun parseKomposerColor(hex: String): Color {
    val digits = hex.removePrefix("#")
    val value = digits.toLongOrNull(16)
        ?: throw KomposerRenderException("Invalid color: $hex")
    return when (digits.length) {
        6 -> Color(0xFF000000L or value)   // six digits imply alpha FF
        8 -> Color(value)
        else -> throw KomposerRenderException("Invalid color: $hex")
    }
}
