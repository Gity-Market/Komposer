package ir.gity.komposer.core.serialization

import ir.gity.komposer.core.model.BoxModel
import ir.gity.komposer.core.model.ColumnModel
import ir.gity.komposer.core.model.ContentScaleValue
import ir.gity.komposer.core.model.ImageModel
import ir.gity.komposer.core.model.RowModel
import ir.gity.komposer.core.model.TextModel
import ir.gity.komposer.core.model.layout.AlignmentValue
import ir.gity.komposer.core.model.layout.HorizontalArrangementValue
import ir.gity.komposer.core.model.layout.VerticalAlignmentValue
import ir.gity.komposer.core.model.modifier.FillMaxSizeModifier
import ir.gity.komposer.core.model.modifier.SizeModifier
import ir.gity.komposer.core.model.modifier.WeightModifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Catalog reference payload: parses, tree asserted field-by-field, re-encode/re-parse equal. */
class CatalogReferenceTest {

    private val serializer = DefaultKomposerSerializer()

    @Test
    fun referencePayloadParsesFieldByField() {
        val document = serializer.parse(CATALOG_REFERENCE_JSON)
        assertEquals(1, document.version)

        val root = document.root as ColumnModel
        assertEquals(12f, root.spacing)
        assertNull(root.verticalArrangement)
        assertEquals(4, root.children.size)

        // Uniform gaps: spacing + cross-axis alignment, no arrangement token.
        val gapped = root.children[0] as RowModel
        assertEquals(8f, gapped.spacing)
        assertNull(gapped.horizontalArrangement)
        assertEquals(VerticalAlignmentValue.Center, gapped.verticalAlignment)
        assertEquals(3, gapped.children.size)

        // Arrangement token, no spacing.
        val spread = root.children[1] as RowModel
        assertEquals(HorizontalArrangementValue.SpaceBetween, spread.horizontalArrangement)
        assertNull(spread.spacing)

        // Bottom-aligned, weighted filler, nested column.
        val weighted = root.children[2] as RowModel
        assertEquals(VerticalAlignmentValue.Bottom, weighted.verticalAlignment)
        assertEquals(listOf(WeightModifier(value = 1f)), (weighted.children[0] as TextModel).modifiers)
        val nested = weighted.children[2] as ColumnModel
        assertEquals(2, nested.children.size)

        // Box: fixed size, bottomEnd, cropped image under an overlay.
        val box = root.children[3] as BoxModel
        assertEquals(listOf(SizeModifier(width = 200f, height = 120f)), box.modifiers)
        assertEquals(AlignmentValue.BottomEnd, box.contentAlignment)
        val image = box.children[0] as ImageModel
        assertEquals("https://picsum.photos/400/240", image.url)
        assertEquals("sample photo", image.contentDescription)
        assertEquals(ContentScaleValue.Crop, image.contentScale)
        assertEquals(listOf(FillMaxSizeModifier()), image.modifiers)
        val overlay = box.children[1] as TextModel
        assertEquals("overlay", overlay.text)
        assertEquals("#FFFFFF", overlay.color)

        // Whole-document equality + encode → parse → equal.
        assertEquals(CATALOG_REFERENCE_DOCUMENT, document)
        assertEquals(document, serializer.parse(serializer.encode(document)))
    }
}
