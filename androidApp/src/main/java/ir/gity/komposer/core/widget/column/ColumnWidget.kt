package ir.gity.komposer.core.widget.column

import androidx.compose.runtime.Composable
import ir.gity.komposer.core.KomposerWidget
import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.column.ColumnModel
import ir.gity.komposer.core.visitor.KomposerWidgetVisitor
import ir.gity.komposer.core.widget.composite.KomposerCompositeWidget
// Concrete Element
class ColumnWidget(
    children: MutableList<KomposerWidget> = mutableListOf(),
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

    override fun toModel(): KomposerModel {
        return ColumnModel(_children.map { it.toModel() })
    }

    @Composable
    override fun Accept(visitor: KomposerWidgetVisitor) {
        visitor.Visit(this)
        _children.forEach { it.Accept(visitor = visitor) }
    }
}
