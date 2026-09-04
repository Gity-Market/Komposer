package ir.gity.komposer.core.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.gity.komposer.core.model.ColumnModel
import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.layout.HorizontalAlignmentValue
import ir.gity.komposer.core.model.layout.VerticalArrangementValue
import ir.gity.komposer.core.model.modifier.KomposerModifier

// Compose-typed storage with defaults matching Compose, the same strategy as
// TextWidget. An immutable value: the mutable add/removeChild API existed for
// the GoF Composite shape, nothing mutates a widget tree after construction, and the round-trip
// law is easier to trust over values.
//
// `spacing` is stored as its own `Dp?`, never folded into `verticalArrangement` — `spacedBy`
// returns an opaque Arrangement whose dp `toModel()` could not recover (see RowWidget).
data class ColumnWidget(
    val children: List<KomposerWidget> = emptyList(),
    val verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    val horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    val spacing: Dp? = null,
    override val modifiers: List<KomposerModifier> = emptyList(),
) : KomposerWidget {

    init {
        // Mirrors the model rule for hand-built widgets.
        require(spacing == null || verticalArrangement == Arrangement.Top) {
            "spacing and a non-default verticalArrangement are mutually exclusive"
        }
    }

    // Modifiers and spacing copy through verbatim (identity); layout fields normalize Compose
    // defaults back to absent so canonical models round-trip to themselves.
    override fun toModel(): KomposerModel {
        return ColumnModel(
            children = children.map { it.toModel() },
            verticalArrangement = verticalArrangement.toValue(),
            horizontalAlignment = horizontalAlignment.toValue(),
            spacing = spacing?.value,
            modifiers = modifiers,
        )
    }

    // Default (Top / Start) → null; unrepresentable arrangements (e.g. hand-built spacedBy) →
    // null, collapsing to absent (same policy as TextAlign's `else -> null`).
    private fun Arrangement.Vertical.toValue(): VerticalArrangementValue? = when (this) {
        Arrangement.Top -> null
        Arrangement.Center -> VerticalArrangementValue.Center
        Arrangement.Bottom -> VerticalArrangementValue.Bottom
        Arrangement.SpaceBetween -> VerticalArrangementValue.SpaceBetween
        Arrangement.SpaceAround -> VerticalArrangementValue.SpaceAround
        Arrangement.SpaceEvenly -> VerticalArrangementValue.SpaceEvenly
        else -> null
    }

    private fun Alignment.Horizontal.toValue(): HorizontalAlignmentValue? = when (this) {
        Alignment.Start -> null
        Alignment.CenterHorizontally -> HorizontalAlignmentValue.Center
        Alignment.End -> HorizontalAlignmentValue.End
        else -> null
    }
}

/** Children map through the one dispatching extension, so nesting cannot diverge. */
fun ColumnModel.toWidget(): ColumnWidget = ColumnWidget(
    children = children.map { it.toWidget() },
    verticalArrangement = verticalArrangement.toArrangement(),
    horizontalAlignment = horizontalAlignment.toAlignment(),
    // dp on the wire: no Density needed, mapping works outside a composition.
    spacing = spacing?.dp,
    // Modifiers copy through unchanged.
    modifiers = modifiers,
)

// Absent (null) → Compose default, matching the "the mapping layer applies Compose defaults"
// rule. Exhaustive `when` over the closed enum.
private fun VerticalArrangementValue?.toArrangement(): Arrangement.Vertical = when (this) {
    null -> Arrangement.Top
    VerticalArrangementValue.Top -> Arrangement.Top
    VerticalArrangementValue.Center -> Arrangement.Center
    VerticalArrangementValue.Bottom -> Arrangement.Bottom
    VerticalArrangementValue.SpaceBetween -> Arrangement.SpaceBetween
    VerticalArrangementValue.SpaceAround -> Arrangement.SpaceAround
    VerticalArrangementValue.SpaceEvenly -> Arrangement.SpaceEvenly
}

private fun HorizontalAlignmentValue?.toAlignment(): Alignment.Horizontal = when (this) {
    null -> Alignment.Start
    HorizontalAlignmentValue.Start -> Alignment.Start
    HorizontalAlignmentValue.Center -> Alignment.CenterHorizontally
    HorizontalAlignmentValue.End -> Alignment.End
}
