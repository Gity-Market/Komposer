package ir.gity.komposer.core.widget.factory

import ir.gity.komposer.core.KomposerWidget
import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.column.ColumnModel
import ir.gity.komposer.core.widget.column.ColumnWidget

class ColumnWidgetFactory : KomposerWidgetFactory {
    override fun create(model: KomposerModel): KomposerWidget {
        require(model is ColumnModel)
        return ColumnWidget(
            children = model.children.map { it.toWidget() }.toMutableList(),
        )
    }
}
