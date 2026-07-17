package ir.gity.komposer.core.model.spacer

import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.KomposerModelVisitor
import ir.gity.komposer.core.model.modifier.KomposerModifier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Maps to `androidx.compose.foundation.layout.Spacer` (SPEC-0002 §3). `height` is in dp
 * — no pixels on the wire, so no `Density` is needed to render it.
 *
 * `modifiers` is the last constructor parameter (SPEC-0005 §4) so existing positional
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

    override fun accept(visitor: KomposerModelVisitor) = visitor.visit(this)
}
