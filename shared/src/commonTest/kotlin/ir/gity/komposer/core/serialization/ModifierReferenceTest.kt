package ir.gity.komposer.core.serialization

import ir.gity.komposer.core.model.ColumnModel
import ir.gity.komposer.core.model.layout.HorizontalAlignmentValue
import ir.gity.komposer.core.model.layout.VerticalArrangementValue
import ir.gity.komposer.core.model.modifier.BackgroundModifier
import ir.gity.komposer.core.model.modifier.FillMaxSizeModifier
import ir.gity.komposer.core.model.modifier.FillMaxWidthModifier
import ir.gity.komposer.core.model.modifier.PaddingModifier
import ir.gity.komposer.core.model.modifier.SizeModifier
import ir.gity.komposer.core.model.modifier.WeightModifier
import ir.gity.komposer.core.model.TextModel
import kotlin.test.Test
import kotlin.test.assertEquals

/** Modifier reference payload: parses, tree asserted field-by-field, re-encode/re-parse equal. */
class ModifierReferenceTest {

    private val serializer = DefaultKomposerSerializer()

    @Test
    fun referencePayloadParsesFieldByField() {
        val document = serializer.parse(MODIFIER_REFERENCE_JSON)
        assertEquals(1, document.version)

        val root = document.root as ColumnModel
        // Root column modifiers, in order.
        assertEquals(
            listOf(
                FillMaxSizeModifier(),
                BackgroundModifier("#F2F2F7"),
                PaddingModifier(all = 16f),
            ),
            root.modifiers,
        )
        assertEquals(HorizontalAlignmentValue.Center, root.horizontalAlignment)
        assertEquals(null, root.verticalArrangement)
        assertEquals(7, root.children.size)

        // The two order-contrast rows differ only by modifier order.
        val bgThenPad = root.children[2] as TextModel
        assertEquals(
            listOf(BackgroundModifier("#FFD54F"), PaddingModifier(horizontal = 12f, vertical = 4f)),
            bgThenPad.modifiers,
        )
        val padThenBg = root.children[3] as TextModel
        assertEquals(
            listOf(PaddingModifier(horizontal = 12f, vertical = 4f), BackgroundModifier("#FFD54F")),
            padThenBg.modifiers,
        )

        // The weighted row.
        val weighted = root.children[5] as TextModel
        assertEquals(
            listOf(WeightModifier(value = 1f), FillMaxWidthModifier(), BackgroundModifier("#E1F5FE")),
            weighted.modifiers,
        )

        // Nested column: fractional fill, fixed height, spaceBetween.
        val nested = root.children[6] as ColumnModel
        assertEquals(
            listOf(
                FillMaxWidthModifier(fraction = 0.5f),
                SizeModifier(height = 120f),
                BackgroundModifier("#EDE7F6"),
                PaddingModifier(all = 8f),
            ),
            nested.modifiers,
        )
        assertEquals(VerticalArrangementValue.SpaceBetween, nested.verticalArrangement)
        assertEquals(2, nested.children.size)

        // Whole-document equality + encode → parse → equal.
        assertEquals(MODIFIER_REFERENCE_DOCUMENT, document)
        assertEquals(document, serializer.parse(serializer.encode(document)))
    }
}
