package ir.gity.komposer.core.serialization

import kotlin.test.Test
import kotlin.test.assertFailsWith

/** One failing-parse test per SPEC-0005 §7 validation rule (modifiers attach to a carrier). */
class ModifierValidationTest {

    private val serializer = DefaultKomposerSerializer()

    /** Wraps a modifier JSON in a carrier node and asserts parsing fails. */
    private fun assertModifierFails(modifierJson: String) =
        assertFailsWith<KomposerParseException> {
            serializer.parseNode("""{"type":"spacer","height":1,"modifiers":[$modifierJson]}""")
        }

    @Test
    fun paddingWithNoFieldsFails() {
        assertModifierFails("""{"type":"padding"}""")
    }

    @Test
    fun paddingFromMoreThanOneGroupFails() {
        assertModifierFails("""{"type":"padding","all":8,"start":4}""")
        assertModifierFails("""{"type":"padding","horizontal":8,"top":4}""")
    }

    @Test
    fun paddingNegativeValueFails() {
        assertModifierFails("""{"type":"padding","all":-1}""")
        assertModifierFails("""{"type":"padding","start":-2}""")
    }

    @Test
    fun sizeWithNoFieldsFails() {
        assertModifierFails("""{"type":"size"}""")
    }

    @Test
    fun sizeNegativeValueFails() {
        assertModifierFails("""{"type":"size","width":-1}""")
        assertModifierFails("""{"type":"size","height":-1}""")
    }

    @Test
    fun fillFractionOutOfRangeFails() {
        assertModifierFails("""{"type":"fillMaxWidth","fraction":1.5}""")
        assertModifierFails("""{"type":"fillMaxHeight","fraction":-0.1}""")
        assertModifierFails("""{"type":"fillMaxSize","fraction":2}""")
    }

    @Test
    fun backgroundBadColorFails() {
        assertModifierFails("""{"type":"background","color":"red"}""")
        assertModifierFails("""{"type":"background","color":"#12345"}""")
    }

    @Test
    fun backgroundMissingColorFails() {
        assertModifierFails("""{"type":"background"}""")
    }

    @Test
    fun weightNonPositiveFails() {
        assertModifierFails("""{"type":"weight","value":0}""")
        assertModifierFails("""{"type":"weight","value":-1}""")
    }

    @Test
    fun weightMissingValueFails() {
        assertModifierFails("""{"type":"weight"}""")
    }
}
