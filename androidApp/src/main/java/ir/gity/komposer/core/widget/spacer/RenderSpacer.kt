package ir.gity.komposer.core.widget.spacer

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun RenderSpacer(widget: SpacerWidget) {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(widget.height)
    )
}
