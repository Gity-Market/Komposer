package ir.gity.komposer.core.widget

import androidx.compose.ui.Alignment
import ir.gity.komposer.core.model.BoxModel
import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.layout.AlignmentValue
import ir.gity.komposer.core.model.modifier.KomposerModifier

// Compose-typed storage with defaults matching Compose (`Alignment.TopStart`), an immutable
// value — the ColumnWidget / RowWidget pattern with a two-dimensional alignment instead of
// per-axis arrangement + alignment.
data class BoxWidget(
    val children: List<KomposerWidget> = emptyList(),
    val contentAlignment: Alignment = Alignment.TopStart,
    override val modifiers: List<KomposerModifier> = emptyList(),
) : KomposerWidget {

    // Modifiers copy through verbatim (identity); the alignment normalizes the Compose default
    // back to absent so canonical models round-trip to themselves.
    override fun toModel(): KomposerModel = BoxModel(
        children = children.map { it.toModel() },
        contentAlignment = contentAlignment.toValue(),
        modifiers = modifiers,
    )

    // Default (TopStart) → null; the eight other named positions → their token; an arbitrary
    // hand-built BiasAlignment → null, collapsing to absent (same policy as the other containers).
    private fun Alignment.toValue(): AlignmentValue? = when (this) {
        Alignment.TopStart -> null
        Alignment.TopCenter -> AlignmentValue.TopCenter
        Alignment.TopEnd -> AlignmentValue.TopEnd
        Alignment.CenterStart -> AlignmentValue.CenterStart
        Alignment.Center -> AlignmentValue.Center
        Alignment.CenterEnd -> AlignmentValue.CenterEnd
        Alignment.BottomStart -> AlignmentValue.BottomStart
        Alignment.BottomCenter -> AlignmentValue.BottomCenter
        Alignment.BottomEnd -> AlignmentValue.BottomEnd
        else -> null
    }
}

/** Children map through the one dispatching extension, so nesting cannot diverge. */
fun BoxModel.toWidget(): BoxWidget = BoxWidget(
    children = children.map { it.toWidget() },
    contentAlignment = contentAlignment.toAlignment(),
    // Modifiers copy through unchanged.
    modifiers = modifiers,
)

// Absent (null) → Compose default. Exhaustive `when` over the closed enum.
private fun AlignmentValue?.toAlignment(): Alignment = when (this) {
    null -> Alignment.TopStart
    AlignmentValue.TopStart -> Alignment.TopStart
    AlignmentValue.TopCenter -> Alignment.TopCenter
    AlignmentValue.TopEnd -> Alignment.TopEnd
    AlignmentValue.CenterStart -> Alignment.CenterStart
    AlignmentValue.Center -> Alignment.Center
    AlignmentValue.CenterEnd -> Alignment.CenterEnd
    AlignmentValue.BottomStart -> Alignment.BottomStart
    AlignmentValue.BottomCenter -> Alignment.BottomCenter
    AlignmentValue.BottomEnd -> Alignment.BottomEnd
}
