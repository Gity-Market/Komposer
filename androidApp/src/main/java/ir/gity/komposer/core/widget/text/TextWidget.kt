package ir.gity.komposer.core.widget.text

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import ir.gity.komposer.core.KomposerWidget
import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.modifier.KomposerModifier
import ir.gity.komposer.core.model.text.FontStyleValue
import ir.gity.komposer.core.model.text.TextAlignValue
import ir.gity.komposer.core.model.text.TextDecorationValue
import ir.gity.komposer.core.model.text.TextModel
import ir.gity.komposer.core.model.text.TextOverflowValue
import ir.gity.komposer.core.visitor.KomposerWidgetVisitor


data class TextWidget(
    val text: String,
    val color: Color = Color.Unspecified,
    val fontSize: TextUnit = TextUnit.Unspecified,
    val fontStyle: FontStyle? = null,
    val fontWeight: FontWeight? = null,
    val fontFamily: FontFamily? = null,
    val letterSpacing: TextUnit = TextUnit.Unspecified,
    val textDecoration: TextDecoration? = null,
    val textAlign: TextAlign? = null,
    val lineHeight: TextUnit = TextUnit.Unspecified,
    val overflow: TextOverflow = TextOverflow.Clip,
    val softWrap: Boolean = true,
    val maxLines: Int = Int.MAX_VALUE,
    val minLines: Int = 1,
    val onTextLayout: ((TextLayoutResult) -> Unit) = {},
    val style: TextStyle = TextStyle.Default,
    override val modifiers: List<KomposerModifier> = emptyList(),
) : KomposerWidget {

    // Faithful, normalized to canonical form (SPEC-0004 §4): values equal to Compose
    // defaults collapse to `null` (absent), so a model round-trips to itself when canonical.
    // `modifiers` is copied through verbatim (SPEC-0005 §5.1) — identity, so exact for every
    // list. `style`, `fontFamily`, `onTextLayout` stay widget-only: no wire counterpart yet.
    // The old opaque `modifier: Modifier` field is gone (SPEC-0005 §5.5): a wire-invisible
    // path `toModel()` silently dropped is exactly the "two competing paths" bug class.
    override fun toModel(): KomposerModel {
        return TextModel(
            text = text,
            color = if (color == Color.Unspecified) null else color.toHexString(),
            fontSize = if (fontSize == TextUnit.Unspecified) null else fontSize.value,
            fontWeight = fontWeight?.weight,
            fontStyle = fontStyle?.let {
                when (it) {
                    FontStyle.Italic -> FontStyleValue.Italic
                    else -> FontStyleValue.Normal
                }
            },
            letterSpacing = if (letterSpacing == TextUnit.Unspecified) null else letterSpacing.value,
            textDecoration = fontDecorationToValue(textDecoration),
            textAlign = textAlignToValue(textAlign),
            lineHeight = if (lineHeight == TextUnit.Unspecified) null else lineHeight.value,
            overflow = when (overflow) {
                TextOverflow.Clip -> null
                TextOverflow.Ellipsis -> TextOverflowValue.Ellipsis
                TextOverflow.Visible -> TextOverflowValue.Visible
                else -> null
            },
            softWrap = if (softWrap) null else false,
            maxLines = if (maxLines == Int.MAX_VALUE) null else maxLines,
            minLines = if (minLines == 1) null else minLines,
            modifiers = modifiers,
        )
    }

    override fun Accept(visitor: KomposerWidgetVisitor) {
        visitor.Visit(this)
    }

    private fun Color.toHexString(): String {
        val argb = toArgb()
        val digits = argb.toUInt().toString(16).padStart(8, '0').uppercase()
        return "#$digits"
    }

    private fun fontDecorationToValue(decoration: TextDecoration?): TextDecorationValue? =
        when (decoration) {
            null -> null
            TextDecoration.Underline -> TextDecorationValue.Underline
            TextDecoration.LineThrough -> TextDecorationValue.LineThrough
            else -> TextDecorationValue.None
        }

    private fun textAlignToValue(align: TextAlign?): TextAlignValue? =
        when (align) {
            null -> null
            TextAlign.Start -> TextAlignValue.Start
            TextAlign.End -> TextAlignValue.End
            TextAlign.Center -> TextAlignValue.Center
            TextAlign.Justify -> TextAlignValue.Justify
            TextAlign.Left -> TextAlignValue.Left
            TextAlign.Right -> TextAlignValue.Right
            else -> null
        }
}
