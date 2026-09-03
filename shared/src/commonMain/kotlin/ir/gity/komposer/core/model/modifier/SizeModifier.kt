package ir.gity.komposer.core.model.modifier

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Maps to `Modifier.width` / `height` / `size` — the constraint-respecting variants, not
 * `required*`. At least one of `width`/`height` must be present.
 */
@Serializable
@SerialName("size")
data class SizeModifier(
    val width: Float? = null,
    val height: Float? = null,
) : KomposerModifier {
    init {
        require(width != null || height != null) { "size requires width and/or height" }
        listOfNotNull(width, height).forEach {
            require(it.isFinite() && it >= 0f) { "size values must be finite and >= 0, was $it" }
        }
    }
}
