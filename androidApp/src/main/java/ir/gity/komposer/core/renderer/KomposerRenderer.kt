package ir.gity.komposer.core.renderer

import androidx.compose.runtime.Composable
import ir.gity.komposer.core.KomposerWidget
import ir.gity.komposer.core.widget.column.ColumnWidget
import ir.gity.komposer.core.widget.column.RenderColumn
import ir.gity.komposer.core.widget.spacer.RenderSpacer
import ir.gity.komposer.core.widget.spacer.SpacerWidget
import ir.gity.komposer.core.widget.text.RenderText
import ir.gity.komposer.core.widget.text.TextWidget

@Composable
fun KomposerRenderer(widget: KomposerWidget) {
    when (widget) {
        is ColumnWidget -> RenderColumn(widget)
        is TextWidget -> RenderText(widget)
        is SpacerWidget -> RenderSpacer(widget)
    }
}