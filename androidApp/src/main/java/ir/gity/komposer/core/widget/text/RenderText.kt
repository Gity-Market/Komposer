package ir.gity.komposer.core.widget.text

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun RenderText(widget: TextWidget) {
    Text(
        text = widget.text,
        modifier = widget.modifier,
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
        onTextLayout = widget.onTextLayout,
        style = widget.style
    )
}
