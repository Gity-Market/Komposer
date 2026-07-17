package ir.gity.komposer.core.widget.column

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import ir.gity.komposer.core.KomposerWidget
import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.column.ColumnModel
import ir.gity.komposer.core.model.layout.HorizontalAlignmentValue
import ir.gity.komposer.core.model.layout.VerticalArrangementValue
import ir.gity.komposer.core.model.modifier.KomposerModifier
import ir.gity.komposer.core.visitor.KomposerWidgetVisitor
import ir.gity.komposer.core.widget.composite.KomposerCompositeWidget

// Concrete Element. Compose-typed storage with defaults matching Compose (SPEC-0005 §5.6),
// the same strategy as TextWidget.
class ColumnWidget(
    children: MutableList<KomposerWidget> = mutableListOf(),
    val verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    val horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    override val modifiers: List<KomposerModifier> = emptyList(),
) : KomposerWidget, KomposerCompositeWidget {
    private val _children: MutableList<KomposerWidget> = children
    override fun addChild(widget: KomposerWidget) {
        _children.add(widget)
    }

    override fun removeChild(widget: KomposerWidget) {
        _children.remove(widget)
    }

    override fun getChildren(): List<KomposerWidget> {
        return _children
    }

    // Modifiers copy through verbatim (identity); layout fields normalize Compose defaults
    // back to absent (SPEC-0005 §6) so canonical models round-trip to themselves.
    override fun toModel(): KomposerModel {
        return ColumnModel(
            children = _children.map { it.toModel() },
            verticalArrangement = verticalArrangement.toValue(),
            horizontalAlignment = horizontalAlignment.toValue(),
            modifiers = modifiers,
        )
    }

    override fun Accept(visitor: KomposerWidgetVisitor) {
        visitor.Visit(this)
        _children.forEach { it.Accept(visitor = visitor) }
    }

    // Default (Top / Start) → null; unrepresentable arrangements (e.g. hand-built spacedBy) →
    // null, collapsing to absent (same policy as TextAlign's `else -> null` in SPEC-0004 §4).
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
