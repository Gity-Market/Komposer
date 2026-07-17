package ir.gity.komposer.core.widget.spacer

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import ir.gity.komposer.core.renderer.KomposerRenderScope
import ir.gity.komposer.core.renderer.toComposeModifier

@Composable
fun RenderSpacer(widget: SpacerWidget, scope: KomposerRenderScope? = null) {
    // Wire modifiers fold first; the node-intrinsic `height` is appended after (SPEC-0005 §5.4),
    // matching Compose's convention that the caller's modifier heads the chain. The old
    // `fillMaxWidth()` hardcode is gone (§5.5): inside a column the vertical gap is unchanged.
    Spacer(modifier = widget.modifiers.toComposeModifier(scope).height(widget.height))
}
