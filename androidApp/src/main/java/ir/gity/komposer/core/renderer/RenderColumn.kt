package ir.gity.komposer.core.renderer

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import ir.gity.komposer.core.widget.ColumnWidget

@Composable
fun RenderColumn(widget: ColumnWidget, scope: KomposerRenderScope? = null) {
    // The `Modifier.fillMaxWidth()` hardcode is gone: a bare column now sizes
    // like a bare Compose Column (wrapping its content). Full-width payloads say so explicitly
    // via a `fillMaxWidth` modifier. Wire modifiers fold with the scope this column received
    // from *its* parent.
    Column(
        modifier = widget.modifiers.toComposeModifier(scope),
        verticalArrangement = widget.verticalArrangement,
        horizontalAlignment = widget.horizontalAlignment,
    ) {
        // A composite creates the weight-capable scope for its children.
        val childScope = ColumnRenderScope(this)
        // Delegate to the single render-dispatch point instead of re-implementing a `when`.
        widget.children.forEach { child ->
            KomposerRenderer(child, childScope)
        }
    }
}
