package ir.gity.komposer.core.widget.text

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import ir.gity.komposer.core.renderer.KomposerRenderScope
import ir.gity.komposer.core.renderer.toComposeModifier

@Composable
fun RenderText(widget: TextWidget, scope: KomposerRenderScope? = null) {
    Text(
        text = widget.text,
        // Wire modifiers, folded with the scope this widget received from its parent
        // (SPEC-0005 §5.1). The old opaque `widget.modifier` field is gone (§5.5).
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
