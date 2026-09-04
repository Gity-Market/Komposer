package ir.gity.komposer.core.model

import ir.gity.komposer.core.model.layout.HorizontalArrangementValue
import ir.gity.komposer.core.model.layout.VerticalAlignmentValue
import ir.gity.komposer.core.model.modifier.KomposerModifier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Maps to `androidx.compose.foundation.layout.Row` — `column`'s horizontal counterpart.
 *
 * `horizontalArrangement` / `verticalAlignment` are **node fields**, not modifiers: in Compose
 * they are `Row` parameters, not `Modifier` calls.
 *
 * `spacing` is `Arrangement.spacedBy(spacing.dp)` on the wire. A *parameterized* arrangement
 * cannot be a closed-enum token, so it travels as a sibling dp field — and because `Row` has
 * exactly one `horizontalArrangement` slot, `spacing` and `horizontalArrangement` are **mutually
 * exclusive** (two arrangements for one slot is a contradiction, not a precedence question).
 *
 * `modifiers` is the last constructor parameter so positional construction sites keep compiling.
 */
@Serializable
@SerialName("row")
data class RowModel(
    val children: List<KomposerModel> = emptyList(),
    val horizontalArrangement: HorizontalArrangementValue? = null,
    val verticalAlignment: VerticalAlignmentValue? = null,
    val spacing: Float? = null,
    override val modifiers: List<KomposerModifier> = emptyList(),
) : KomposerModel {
    init {
        spacing?.let {
            require(it.isFinite() && it >= 0f) { "spacing must be finite and >= 0, was $it" }
            require(horizontalArrangement == null) {
                "spacing and horizontalArrangement are mutually exclusive (spacing IS an arrangement)"
            }
        }
    }
}
