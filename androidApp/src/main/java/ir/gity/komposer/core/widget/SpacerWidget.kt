package ir.gity.komposer.core.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.SpacerModel
import ir.gity.komposer.core.model.modifier.KomposerModifier

data class SpacerWidget(
    val height: Dp,
    override val modifiers: List<KomposerModifier> = emptyList(),
) : KomposerWidget {
    // Faithful: the real height in dp (was a hardcoded 26f); modifiers copied through verbatim.
    override fun toModel(): KomposerModel = SpacerModel(height = height.value, modifiers = modifiers)
}

/**
 * dp on the wire means no `Density` conversion — so mapping works outside a composition.
 * Modifiers copy through unchanged.
 */
fun SpacerModel.toWidget(): SpacerWidget = SpacerWidget(height = height.dp, modifiers = modifiers)
