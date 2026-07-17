package ir.gity.komposer.core.renderer

import androidx.compose.runtime.Composable
import ir.gity.komposer.core.KomposerRenderException
import ir.gity.komposer.core.KomposerWidget
import ir.gity.komposer.core.widget.column.ColumnWidget
import ir.gity.komposer.core.widget.column.RenderColumn
import ir.gity.komposer.core.widget.spacer.RenderSpacer
import ir.gity.komposer.core.widget.spacer.SpacerWidget
import ir.gity.komposer.core.widget.text.RenderText
import ir.gity.komposer.core.widget.text.TextWidget

// The single render-dispatch point (SPEC-0004 §5). Still a `when` by choice until Phase 4.
// `scope` is the weight-capable scope handed down by the parent composite; `null` at the root
// (SPEC-0005 §5.3), where a `weight` modifier therefore fails loudly.
@Composable
fun KomposerRenderer(widget: KomposerWidget, scope: KomposerRenderScope? = null) {
    when (widget) {
        is ColumnWidget -> RenderColumn(widget, scope)
        is TextWidget -> RenderText(widget, scope)
        is SpacerWidget -> RenderSpacer(widget, scope)
        else -> throw KomposerRenderException(
            "No render branch for ${widget::class.simpleName}"
        )
    }
}
