package ir.gity.komposer.core.renderer

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import ir.gity.komposer.core.widget.BoxWidget

@Composable
fun RenderBox(widget: BoxWidget, scope: KomposerRenderScope? = null) {
    // A bare box wraps its content like a bare Compose Box. Wire modifiers fold with the scope
    // this box received from *its* parent.
    Box(
        modifier = widget.modifiers.toComposeModifier(scope),
        contentAlignment = widget.contentAlignment,
    ) {
        // BoxScope has no `weight`, so children get **no** scope: a `weight` on a direct child
        // of a box fails loudly at the fold — the first mid-tree position where that rule bites.
        widget.children.forEach { child ->
            KomposerRenderer(child, scope = null)
        }
    }
}
