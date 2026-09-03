package ir.gity.komposer.core.serialization

import ir.gity.komposer.core.model.TextModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** One dedicated failing-input test per strictness rule. */
class StrictnessTest {

    private val serializer = DefaultKomposerSerializer()

    @Test
    fun unknownTypeFails() {
        assertFailsWith<KomposerParseException> {
            serializer.parseNode("""{"type":"blink","text":"hi"}""")
        }
    }

    @Test
    fun unknownFieldOnKnownNodeIsIgnored() {
        val node = serializer.parseNode("""{"type":"text","text":"hi","glow":true}""")
        assertEquals(TextModel(text = "hi"), node)
    }

    @Test
    fun explicitNullForOptionalFieldIsAcceptedAsAbsent() {
        val node = serializer.parseNode("""{"type":"text","text":"hi","color":null}""")
        assertEquals(TextModel(text = "hi", color = null), node)
    }

    @Test
    fun missingRequiredTextFails() {
        assertFailsWith<KomposerParseException> {
            serializer.parseNode("""{"type":"text"}""")
        }
    }

    @Test
    fun missingRequiredSpacerHeightFails() {
        assertFailsWith<KomposerParseException> {
            serializer.parseNode("""{"type":"spacer"}""")
        }
    }

    @Test
    fun missingRequiredVersionFails() {
        assertFailsWith<KomposerParseException> {
            serializer.parse("""{"root":{"type":"text","text":"hi"}}""")
        }
    }

    @Test
    fun unsupportedVersionFails() {
        val ex = assertFailsWith<KomposerParseException> {
            serializer.parse("""{"version":2,"root":{"type":"text","text":"hi"}}""")
        }
        assertEquals("Unsupported wire version: 2", ex.message)
    }
}
