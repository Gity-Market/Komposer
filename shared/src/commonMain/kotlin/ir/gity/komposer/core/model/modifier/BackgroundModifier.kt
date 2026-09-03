package ir.gity.komposer.core.model.modifier

import ir.gity.komposer.core.model.WireColor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Maps to `Modifier.background(color)`. Solid color only in v1; a shape
 * vocabulary is a deferred design (Open questions).
 */
@Serializable
@SerialName("background")
data class BackgroundModifier(val color: String) : KomposerModifier {
    init {
        require(WireColor.REGEX.matches(color)) {
            "color must match #RRGGBB or #AARRGGBB, was \"$color\""
        }
    }
}
