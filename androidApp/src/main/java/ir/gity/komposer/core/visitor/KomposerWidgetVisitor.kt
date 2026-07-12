package ir.gity.komposer.core.visitor

import ir.gity.komposer.core.KomposerWidget
import ir.gity.komposer.core.widget.column.ColumnWidget
import ir.gity.komposer.core.widget.spacer.SpacerWidget
import ir.gity.komposer.core.widget.text.TextWidget

// Visitor — no @Composable: traversal builds data, not UI (SPEC-0004 §5).
interface KomposerWidgetVisitor {
    fun Visit(widget: KomposerWidget)
    fun Visit(textWidget: TextWidget)
    fun Visit(columnWidget: ColumnWidget)
    fun Visit(spacerWidget: SpacerWidget)
}

// Concrete Visitor
class GraphBuilder : KomposerWidgetVisitor {
    private val stringBuilder = StringBuilder()

    override fun Visit(widget: KomposerWidget) {
        when (widget) {
            is TextWidget -> Visit(widget)
            is ColumnWidget -> Visit(widget)
            is SpacerWidget -> Visit(widget)
        }
    }

    override fun Visit(textWidget: TextWidget) {
        stringBuilder.appendLine("Text Widget(text: ${textWidget.text})")
    }

    override fun Visit(columnWidget: ColumnWidget) {
        stringBuilder.appendLine("Column(size: ${columnWidget.getChildren().size})")
    }

    override fun Visit(spacerWidget: SpacerWidget) {
        stringBuilder.appendLine("Spacer(value: ${spacerWidget.height})")
    }

    fun build() = stringBuilder.toString()
}
