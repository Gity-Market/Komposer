package ir.gity.komposer.core.widget.factory

import ir.gity.komposer.core.KomposerWidget
import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.column.ColumnModel
import ir.gity.komposer.core.widget.column.ColumnWidget

class ColumnWidgetFactory : KomposerWidgetFactory {
    override fun create(model: KomposerModel, root: KomposerWidgetFactory): KomposerWidget {
        require(model is ColumnModel) { "ColumnWidgetFactory received ${model::class.simpleName}" }
        // Recurse through the dispatching factory so a custom factory registered for a
        // child type applies to nested children too (fixes the silent-bypass bug).
        return ColumnWidget(
            children = model.children.map { root.create(it, root) }.toMutableList(),
        )
    }
}
