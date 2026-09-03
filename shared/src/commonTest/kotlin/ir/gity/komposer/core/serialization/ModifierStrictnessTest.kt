package ir.gity.komposer.core.serialization

import ir.gity.komposer.core.model.TextModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Strictness rows for the `modifiers` field. */
class ModifierStrictnessTest {

    private val serializer = DefaultKomposerSerializer()

    @Test
    fun unknownModifierTypeFails() {
        assertFailsWith<KomposerParseException> {
            serializer.parseNode("""{"type":"text","text":"x","modifiers":[{"type":"blink"}]}""")
        }
    }

    @Test
    fun nullModifiersListFails() {
        // The property is a non-nullable list with a default: it must be omitted, not nulled.
        assertFailsWith<KomposerParseException> {
            serializer.parseNode("""{"type":"text","text":"x","modifiers":null}""")
        }
    }

    @Test
    fun emptyModifiersListEqualsAbsent() {
        val withEmpty = serializer.parseNode("""{"type":"text","text":"x","modifiers":[]}""")
        assertEquals(TextModel(text = "x"), withEmpty)
    }

    @Test
    fun unknownFieldOnKnownModifierIsIgnored() {
        // ignoreUnknownKeys is global — unknown modifier fields are dropped.
        val node = serializer.parseNode(
            """{"type":"text","text":"x","modifiers":[{"type":"background","color":"#FFFFFF","glow":true}]}""",
        )
        assertEquals(
            TextModel(
                text = "x",
                modifiers = listOf(ir.gity.komposer.core.model.modifier.BackgroundModifier("#FFFFFF")),
            ),
            node,
        )
    }
}
