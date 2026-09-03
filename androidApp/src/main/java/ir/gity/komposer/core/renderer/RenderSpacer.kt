package ir.gity.komposer.core.renderer

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import ir.gity.komposer.core.widget.SpacerWidget

@Composable
fun RenderSpacer(widget: SpacerWidget, scope: KomposerRenderScope? = null) {
    // Wire modifiers fold first; the node-intrinsic `height` is appended after,
    // matching Compose's convention that the caller's modifier heads the chain. The old
    // `fillMaxWidth()` hardcode is gone: inside a column the vertical gap is unchanged.
    Spacer(modifier = widget.modifiers.toComposeModifier(scope).height(widget.height))
}
