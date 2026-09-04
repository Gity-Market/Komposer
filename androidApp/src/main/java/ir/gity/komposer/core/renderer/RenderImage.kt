package ir.gity.komposer.core.renderer

import androidx.compose.runtime.Composable
import coil3.compose.AsyncImage
import ir.gity.komposer.core.widget.ImageWidget

@Composable
fun RenderImage(widget: ImageWidget, scope: KomposerRenderScope? = null) {
    // Coil resolves the URL asynchronously through the singleton ImageLoader; the OkHttp network
    // fetcher (`coil-network-okhttp`) registers itself from the classpath, so there is no loader
    // setup here. Wire modifiers fold with the scope this image received from its parent —
    // sizing is theirs (`size`, `fillMaxWidth`, …); the node itself adds nothing intrinsic.
    AsyncImage(
        model = widget.url,
        contentDescription = widget.contentDescription,
        modifier = widget.modifiers.toComposeModifier(scope),
        contentScale = widget.contentScale,
    )
}
