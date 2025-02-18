package ir.gity.komposer.core.widget.column

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ir.gity.komposer.core.widget.spacer.RenderSpacer
import ir.gity.komposer.core.widget.spacer.SpacerWidget
import ir.gity.komposer.core.widget.text.RenderText
import ir.gity.komposer.core.widget.text.TextWidget

@Composable
fun RenderColumn(widget: ColumnWidget) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        widget.getChildren().forEach { child ->
            when (child) {
                is TextWidget -> RenderText(child)
                is ColumnWidget -> RenderColumn(child)
                is SpacerWidget -> RenderSpacer(child)
            }
        }
    }
}

