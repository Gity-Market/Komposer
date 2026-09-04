package ir.gity.komposer.core.renderer

import androidx.compose.runtime.Composable
import ir.gity.komposer.core.widget.ColumnWidget
import ir.gity.komposer.core.widget.KomposerWidget
import ir.gity.komposer.core.widget.RowWidget
import ir.gity.komposer.core.widget.SpacerWidget
import ir.gity.komposer.core.widget.TextWidget

// The single render-dispatch point. It stays a `when` permanently, now
// compiler-checked: `KomposerWidget` is sealed, so there is **no `else`** — a new
// widget without a render branch is a compile error, strictly stronger than the runtime throw it
// replaced. `scope` is the weight-capable scope handed down by the parent composite; `null` at the
// root, where a `weight` modifier therefore fails loudly.
@Composable
fun KomposerRenderer(widget: KomposerWidget, scope: KomposerRenderScope? = null) {
    when (widget) {
        is ColumnWidget -> RenderColumn(widget, scope)
        is RowWidget -> RenderRow(widget, scope)
        is TextWidget -> RenderText(widget, scope)
        is SpacerWidget -> RenderSpacer(widget, scope)
    }
}
