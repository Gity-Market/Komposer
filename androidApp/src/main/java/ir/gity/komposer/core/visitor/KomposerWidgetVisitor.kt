package ir.gity.komposer.core.visitor

import androidx.compose.runtime.Composable
import ir.gity.komposer.core.KomposerWidget
import ir.gity.komposer.core.widget.column.ColumnWidget
import ir.gity.komposer.core.widget.spacer.SpacerWidget
import ir.gity.komposer.core.widget.text.TextWidget

// Visitor
interface KomposerWidgetVisitor {
    @Composable
    fun Visit(widget: KomposerWidget)

    @Composable
    fun Visit(textWidget: TextWidget)

    @Composable
    fun Visit(columnWidget: ColumnWidget)

    @Composable
    fun Visit(spacerWidget: SpacerWidget)
}

// Concrete Visitor1
class GraphBuilder : KomposerWidgetVisitor {
    private val stringBuilder = StringBuilder()

    @Composable
    override fun Visit(widget: KomposerWidget) {
        when (widget) {
            is TextWidget -> Visit(widget)
            is ColumnWidget -> Visit(widget)
            is SpacerWidget -> Visit(widget)
        }
    }

    @Composable
    override fun Visit(textWidget: TextWidget) {
        stringBuilder.appendLine(
            "Text Widget(" +
                    "text: ${textWidget.text}" +
                    ")"
        )
    }

    @Composable
    override fun Visit(columnWidget: ColumnWidget) {
        stringBuilder.appendLine(
            "Column(" +
                    "size: ${columnWidget.getChildren().size}" +
                    ")"
        )
    }

    @Composable
    override fun Visit(spacerWidget: SpacerWidget) {
        stringBuilder.appendLine(
            "Spacer(" +
                    "value: ${spacerWidget.pxDp}" +
                    ")"
        )
    }

    fun build() = stringBuilder.toString()

}