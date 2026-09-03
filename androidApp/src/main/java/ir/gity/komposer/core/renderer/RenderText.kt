package ir.gity.komposer.core.renderer

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import ir.gity.komposer.core.widget.TextWidget

@Composable
fun RenderText(widget: TextWidget, scope: KomposerRenderScope? = null) {
    Text(
        text = widget.text,
        // Wire modifiers, folded with the scope this widget received from its parent.
        // The old opaque `widget.modifier` field is gone.
        modifier = widget.modifiers.toComposeModifier(scope),
        color = widget.color,
        fontSize = widget.fontSize,
        fontStyle = widget.fontStyle,
        fontWeight = widget.fontWeight,
        fontFamily = widget.fontFamily,
        letterSpacing = widget.letterSpacing,
        textDecoration = widget.textDecoration,
        textAlign = widget.textAlign,
        lineHeight = widget.lineHeight,
        overflow = widget.overflow,
        softWrap = widget.softWrap,
        maxLines = widget.maxLines,
        minLines = widget.minLines,
        onTextLayout = widget.onTextLayout,
        style = widget.style
    )
}
