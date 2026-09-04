package ir.gity.komposer.core.serialization

import ir.gity.komposer.core.model.ColumnModel
import ir.gity.komposer.core.model.RowModel
import ir.gity.komposer.core.model.SpacerModel
import ir.gity.komposer.core.model.FontStyleValue
import ir.gity.komposer.core.model.TextAlignValue
import ir.gity.komposer.core.model.TextDecorationValue
import ir.gity.komposer.core.model.TextModel
import ir.gity.komposer.core.model.TextOverflowValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RoundTripTest {

    private val serializer = DefaultKomposerSerializer()

    private fun assertNodeRoundTrips(model: TextModel) =
        assertEquals(model, serializer.parseNode(serializer.encodeNode(model)))

    @Test
    fun minimalNodesRoundTrip() {
        val nodes = listOf(
            TextModel(text = "hi"),
            ColumnModel(),
            RowModel(),
            SpacerModel(height = 8f),
        )
        for (node in nodes) {
            assertEquals(node, serializer.parseNode(serializer.encodeNode(node)))
        }
    }

    @Test
    fun fullyPopulatedTextRoundTrips() {
        val model = TextModel(
            text = "everything",
            color = "#FF6200EE",
            fontSize = 18f,
            fontWeight = 600,
            fontStyle = FontStyleValue.Italic,
            letterSpacing = 0.5f,
            textDecoration = TextDecorationValue.Underline,
            textAlign = TextAlignValue.Justify,
            lineHeight = 24f,
            overflow = TextOverflowValue.Ellipsis,
            softWrap = false,
            maxLines = 3,
            minLines = 2,
        )
        assertNodeRoundTrips(model)
    }

    @Test
    fun referenceDocumentParsesFieldByField() {
        val document = serializer.parse(REFERENCE_JSON)
        assertEquals(1, document.version)

        val root = document.root as ColumnModel
        assertEquals(4, root.children.size)

        val first = root.children[0] as TextModel
        assertEquals("Hello Komposer", first.text)
        assertEquals(700, first.fontWeight)
        assertEquals(20f, first.fontSize)
        assertEquals("#6200EE", first.color)

        val second = root.children[1] as TextModel
        assertEquals(1, second.maxLines)
        assertEquals(TextOverflowValue.Ellipsis, second.overflow)

        val spacer = root.children[2] as SpacerModel
        assertEquals(16f, spacer.height)

        val nested = root.children[3] as ColumnModel
        val nestedText = nested.children.single() as TextModel
        assertEquals("Nested, italic", nestedText.text)
        assertEquals(FontStyleValue.Italic, nestedText.fontStyle)

        // Whole-document equality plus encode → parse → equal.
        assertEquals(REFERENCE_DOCUMENT, document)
        assertEquals(document, serializer.parse(serializer.encode(document)))
    }

    @Test
    fun nestedTreePreservesOrderAndDepth() {
        val document = REFERENCE_DOCUMENT.copy(
            root = ColumnModel(
                children = listOf(
                    TextModel(text = "a"),
                    ColumnModel(
                        children = listOf(
                            TextModel(text = "b"),
                            ColumnModel(children = listOf(TextModel(text = "c"))),
                        ),
                    ),
                ),
            ),
        )
        assertEquals(document, serializer.parse(serializer.encode(document)))
    }

    @Test
    fun defaultTextEncodesWithoutOptionalKeysOrNulls() {
        val encoded = serializer.encodeNode(TextModel(text = "hi"))
        assertEquals("""{"type":"text","text":"hi"}""", encoded)
        assertFalse("null" in encoded)
    }

    @Test
    fun emptyColumnEncodesWithoutChildrenKey() {
        assertEquals("""{"type":"column"}""", serializer.encodeNode(ColumnModel()))
    }

    @Test
    fun emptyRowEncodesWithoutChildrenKey() {
        assertEquals("""{"type":"row"}""", serializer.encodeNode(RowModel()))
    }
}
