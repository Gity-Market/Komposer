package ir.gity.komposer.core.visitor

import androidx.compose.runtime.Composable
import ir.gity.komposer.core.widget.column.ColumnWidget
import ir.gity.komposer.core.widget.spacer.SpacerWidget
import ir.gity.komposer.core.widget.text.TextWidget


interface KomposerWidgetVisitor {
    @Composable
    fun Visit(textWidget: TextWidget)

    @Composable
    fun Visit(columnWidget: ColumnWidget)

    @Composable
    fun Visit(spacerWidget: SpacerWidget)
}