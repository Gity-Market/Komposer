package ir.gity.komposer.core.widget.column

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ir.gity.komposer.core.renderer.KomposerRenderer

@Composable
fun RenderColumn(widget: ColumnWidget) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Delegate to the single render-dispatch point instead of re-implementing a `when`.
        widget.getChildren().forEach { child ->
            KomposerRenderer(child)
        }
    }
}
