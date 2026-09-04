package ir.gity.komposer.core.renderer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import ir.gity.komposer.core.widget.RowWidget

@Composable
fun RenderRow(widget: RowWidget, scope: KomposerRenderScope? = null) {
    // A bare row wraps its content like a bare Compose Row; full width is stated on the wire
    // via a `fillMaxWidth` modifier, never hardcoded. Wire modifiers fold with the scope this
    // row received from *its* parent. `spacing` and the arrangement are mutually exclusive on
    // the widget, so composing them here is a plain "gap if present, else the token".
    Row(
        modifier = widget.modifiers.toComposeModifier(scope),
        horizontalArrangement = widget.spacing?.let { Arrangement.spacedBy(it) }
            ?: widget.horizontalArrangement,
        verticalAlignment = widget.verticalAlignment,
    ) {
        // A composite creates the weight-capable scope for its children.
        val childScope = RowRenderScope(this)
        // Delegate to the single render-dispatch point instead of re-implementing a `when`.
        widget.children.forEach { child ->
            KomposerRenderer(child, childScope)
        }
    }
}
