package ir.gity.komposer.core.model

import ir.gity.komposer.core.model.modifier.KomposerModifier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Maps to `androidx.compose.foundation.layout.Spacer`. `height` is in dp
 * — no pixels on the wire, so no `Density` is needed to render it.
 *
 * `modifiers` is the last constructor parameter so existing positional
 * construction sites keep compiling.
 */
@Serializable
@SerialName("spacer")
data class SpacerModel(
    val height: Float,
    override val modifiers: List<KomposerModifier> = emptyList(),
) : KomposerModel {
    init {
        require(height.isFinite() && height >= 0f) { "spacer height must be >= 0, was $height" }
    }
}
