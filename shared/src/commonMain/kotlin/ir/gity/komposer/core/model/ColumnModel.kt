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
 * Compose they are `Column` parameters, not `Modifier` calls. `spacing` is
 * `Arrangement.spacedBy(spacing.dp)` — the same sibling-dp-field decision as `RowModel`, and
 * mutually exclusive with `verticalArrangement` for the same reason (one arrangement slot).
 * `modifiers` is the last constructor parameter so positional construction sites keep compiling.
 */
@Serializable
@SerialName("column")
data class ColumnModel(
    val children: List<KomposerModel> = emptyList(),
    val verticalArrangement: VerticalArrangementValue? = null,
    val horizontalAlignment: HorizontalAlignmentValue? = null,
    val spacing: Float? = null,
    override val modifiers: List<KomposerModifier> = emptyList(),
) : KomposerModel {
    init {
        spacing?.let {
            require(it.isFinite() && it >= 0f) { "spacing must be finite and >= 0, was $it" }
            require(verticalArrangement == null) {
                "spacing and verticalArrangement are mutually exclusive (spacing IS an arrangement)"
            }
        }
    }
}
