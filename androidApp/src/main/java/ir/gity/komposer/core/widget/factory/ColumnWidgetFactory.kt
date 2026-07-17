package ir.gity.komposer.core.widget.factory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import ir.gity.komposer.core.KomposerWidget
import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.column.ColumnModel
import ir.gity.komposer.core.model.layout.HorizontalAlignmentValue
import ir.gity.komposer.core.model.layout.VerticalArrangementValue
import ir.gity.komposer.core.widget.column.ColumnWidget

class ColumnWidgetFactory : KomposerWidgetFactory {
    override fun create(model: KomposerModel, root: KomposerWidgetFactory): KomposerWidget {
        require(model is ColumnModel) { "ColumnWidgetFactory received ${model::class.simpleName}" }
        // Recurse through the dispatching factory so a custom factory registered for a
        // child type applies to nested children too (fixes the silent-bypass bug).
        return ColumnWidget(
            children = model.children.map { root.create(it, root) }.toMutableList(),
            verticalArrangement = model.verticalArrangement.toArrangement(),
            horizontalAlignment = model.horizontalAlignment.toAlignment(),
            // Modifiers copy through unchanged (SPEC-0005 §5.1).
            modifiers = model.modifiers,
        )
    }

    // Absent (null) → Compose default, matching the "factory applies Compose defaults" rule.
    // Exhaustive `when` over the closed enum (the TextWidgetFactory pattern).
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
}
