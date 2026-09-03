package ir.gity.komposer.core.model

import ir.gity.komposer.core.model.layout.HorizontalAlignmentValue
import ir.gity.komposer.core.model.layout.VerticalArrangementValue
import ir.gity.komposer.core.model.modifier.KomposerModifier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Maps to `androidx.compose.foundation.layout.Column`.
 *
 * Plain `List<KomposerModel>` — the sealed base carries its own closed polymorphic serializer, so
 * children serialize with the `"type"` discriminator and no registration; the old `@Contextual`
 * annotation actively routed them away from that and is removed.
 *
 * `verticalArrangement` / `horizontalAlignment` are **node fields**, not modifiers: in
 * Compose they are `Column` parameters, not `Modifier` calls. `modifiers` is
 * the last constructor parameter so positional construction sites keep compiling.
 */
@Serializable
@SerialName("column")
data class ColumnModel(
    val children: List<KomposerModel> = emptyList(),
    val verticalArrangement: VerticalArrangementValue? = null,
    val horizontalAlignment: HorizontalAlignmentValue? = null,
    override val modifiers: List<KomposerModifier> = emptyList(),
) : KomposerModel

