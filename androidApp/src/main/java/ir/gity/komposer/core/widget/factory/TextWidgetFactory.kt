package ir.gity.komposer.core.widget.factory

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import ir.gity.komposer.core.KomposerWidget
import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.text.FontStyleValue
import ir.gity.komposer.core.model.text.TextAlignValue
import ir.gity.komposer.core.model.text.TextDecorationValue
import ir.gity.komposer.core.model.text.TextModel
import ir.gity.komposer.core.model.text.TextOverflowValue
import ir.gity.komposer.core.widget.text.TextWidget

/** Maps every SPEC-0002 §1 field, applying Compose defaults for absent ones. */
class TextWidgetFactory : KomposerWidgetFactory {
    override fun create(model: KomposerModel, root: KomposerWidgetFactory): KomposerWidget {
        require(model is TextModel) { "TextWidgetFactory received ${model::class.simpleName}" }
        return TextWidget(
            text = model.text,
            color = model.color?.let { parseKomposerColor(it) } ?: Color.Unspecified,
            fontSize = model.fontSize?.sp ?: TextUnit.Unspecified,
            fontWeight = model.fontWeight?.let { FontWeight(it) },
            fontStyle = model.fontStyle?.let {
                when (it) {
                    FontStyleValue.Normal -> FontStyle.Normal
                    FontStyleValue.Italic -> FontStyle.Italic
                }
            },
            letterSpacing = model.letterSpacing?.sp ?: TextUnit.Unspecified,
            textDecoration = model.textDecoration?.let {
                when (it) {
                    TextDecorationValue.None -> TextDecoration.None
                    TextDecorationValue.Underline -> TextDecoration.Underline
                    TextDecorationValue.LineThrough -> TextDecoration.LineThrough
                }
            },
            textAlign = model.textAlign?.let {
                when (it) {
                    TextAlignValue.Start -> TextAlign.Start
                    TextAlignValue.End -> TextAlign.End
                    TextAlignValue.Center -> TextAlign.Center
                    TextAlignValue.Justify -> TextAlign.Justify
                    TextAlignValue.Left -> TextAlign.Left
                    TextAlignValue.Right -> TextAlign.Right
                }
            },
            lineHeight = model.lineHeight?.sp ?: TextUnit.Unspecified,
            overflow = model.overflow?.let {
                when (it) {
                    TextOverflowValue.Clip -> TextOverflow.Clip
                    TextOverflowValue.Ellipsis -> TextOverflow.Ellipsis
                    TextOverflowValue.Visible -> TextOverflow.Visible
                }
            } ?: TextOverflow.Clip,
            softWrap = model.softWrap ?: true,
            maxLines = model.maxLines ?: Int.MAX_VALUE,
            minLines = model.minLines ?: 1,
            // The factory's whole modifier job is a faithful copy (SPEC-0005 §5.1).
            modifiers = model.modifiers,
        )
    }
}
