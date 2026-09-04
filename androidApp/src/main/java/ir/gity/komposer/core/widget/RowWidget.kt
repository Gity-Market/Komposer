package ir.gity.komposer.core.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.RowModel
import ir.gity.komposer.core.model.layout.HorizontalArrangementValue
import ir.gity.komposer.core.model.layout.VerticalAlignmentValue
import ir.gity.komposer.core.model.modifier.KomposerModifier

// The ColumnWidget pattern on the other axis: Compose-typed storage with defaults matching
// Compose, as an immutable value.
//
// `spacing` is stored as its own `Dp?`, never folded into `horizontalArrangement`:
// `Arrangement.spacedBy` returns an opaque implementation whose dp `toModel()` could not
// recover — the same "folded black box" argument that keeps modifiers stored as the model list.
// The render site composes the two (`RenderRow`).
data class RowWidget(
    val children: List<KomposerWidget> = emptyList(),
    val horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    val verticalAlignment: Alignment.Vertical = Alignment.Top,
    val spacing: Dp? = null,
    override val modifiers: List<KomposerModifier> = emptyList(),
) : KomposerWidget {

    init {
        // Mirrors the model rule, so a hand-built widget cannot reach toModel() with a
        // contradiction the model's init would reject.
        require(spacing == null || horizontalArrangement == Arrangement.Start) {
            "spacing and a non-default horizontalArrangement are mutually exclusive"
        }
    }

    // Modifiers and spacing copy through verbatim (identity); layout fields normalize Compose
    // defaults back to absent so canonical models round-trip to themselves.
    override fun toModel(): KomposerModel = RowModel(
        children = children.map { it.toModel() },
        horizontalArrangement = horizontalArrangement.toValue(),
        verticalAlignment = verticalAlignment.toValue(),
        spacing = spacing?.value,
        modifiers = modifiers,
    )

    // Default (Start / Top) → null; unrepresentable arrangements (e.g. hand-built spacedBy) →
    // null, collapsing to absent (same policy as ColumnWidget).
    private fun Arrangement.Horizontal.toValue(): HorizontalArrangementValue? = when (this) {
        Arrangement.Start -> null
        Arrangement.Center -> HorizontalArrangementValue.Center
        Arrangement.End -> HorizontalArrangementValue.End
        Arrangement.SpaceBetween -> HorizontalArrangementValue.SpaceBetween
        Arrangement.SpaceAround -> HorizontalArrangementValue.SpaceAround
        Arrangement.SpaceEvenly -> HorizontalArrangementValue.SpaceEvenly
        else -> null
    }

    private fun Alignment.Vertical.toValue(): VerticalAlignmentValue? = when (this) {
        Alignment.Top -> null
        Alignment.CenterVertically -> VerticalAlignmentValue.Center
        Alignment.Bottom -> VerticalAlignmentValue.Bottom
        else -> null
    }
}

/** Children map through the one dispatching extension, so nesting cannot diverge. */
fun RowModel.toWidget(): RowWidget = RowWidget(
    children = children.map { it.toWidget() },
    horizontalArrangement = horizontalArrangement.toArrangement(),
    verticalAlignment = verticalAlignment.toAlignment(),
    // dp on the wire: no Density needed, mapping works outside a composition.
    spacing = spacing?.dp,
    // Modifiers copy through unchanged.
    modifiers = modifiers,
)

// Absent (null) → Compose default, matching the "the mapping layer applies Compose defaults"
// rule. Exhaustive `when` over the closed enum.
private fun HorizontalArrangementValue?.toArrangement(): Arrangement.Horizontal = when (this) {
    null -> Arrangement.Start
    HorizontalArrangementValue.Start -> Arrangement.Start
    HorizontalArrangementValue.Center -> Arrangement.Center
    HorizontalArrangementValue.End -> Arrangement.End
    HorizontalArrangementValue.SpaceBetween -> Arrangement.SpaceBetween
    HorizontalArrangementValue.SpaceAround -> Arrangement.SpaceAround
    HorizontalArrangementValue.SpaceEvenly -> Arrangement.SpaceEvenly
}

private fun VerticalAlignmentValue?.toAlignment(): Alignment.Vertical = when (this) {
    null -> Alignment.Top
    VerticalAlignmentValue.Top -> Alignment.Top
    VerticalAlignmentValue.Center -> Alignment.CenterVertically
    VerticalAlignmentValue.Bottom -> Alignment.Bottom
}
