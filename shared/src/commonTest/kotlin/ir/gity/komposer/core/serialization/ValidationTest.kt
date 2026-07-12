package ir.gity.komposer.core.serialization

import kotlin.test.Test
import kotlin.test.assertFailsWith

/** One failing-parse test per SPEC-0002 §4 validation rule. */
class ValidationTest {

    private val serializer = DefaultKomposerSerializer()

    private fun assertParseFails(json: String) =
        assertFailsWith<KomposerParseException> { serializer.parseNode(json) }

    @Test
    fun badColorFormatFails() {
        assertParseFails("""{"type":"text","text":"x","color":"6200EE"}""")   // missing #
        assertParseFails("""{"type":"text","text":"x","color":"#12345"}""")   // wrong length
        assertParseFails("""{"type":"text","text":"x","color":"#gggggg"}""")  // non-hex
    }

    @Test
    fun fontWeightOutOfRangeFails() {
        assertParseFails("""{"type":"text","text":"x","fontWeight":0}""")
        assertParseFails("""{"type":"text","text":"x","fontWeight":1001}""")
    }

    @Test
    fun nonPositiveFontSizeFails() {
        assertParseFails("""{"type":"text","text":"x","fontSize":0}""")
        assertParseFails("""{"type":"text","text":"x","fontSize":-4}""")
    }

    @Test
    fun nonPositiveLineHeightFails() {
        assertParseFails("""{"type":"text","text":"x","lineHeight":0}""")
    }

    @Test
    fun nonFiniteLetterSpacingFails() {
        assertParseFails("""{"type":"text","text":"x","letterSpacing":"NaN"}""")
    }

    @Test
    fun negativeLetterSpacingIsLegal() {
        // Negative tracking is legal typography — must NOT fail.
        serializer.parseNode("""{"type":"text","text":"x","letterSpacing":-0.5}""")
    }

    @Test
    fun subUnitLinesFail() {
        assertParseFails("""{"type":"text","text":"x","maxLines":0}""")
        assertParseFails("""{"type":"text","text":"x","minLines":0}""")
    }

    @Test
    fun maxLinesLessThanMinLinesFails() {
        assertParseFails("""{"type":"text","text":"x","maxLines":1,"minLines":3}""")
    }

    @Test
    fun negativeSpacerHeightFails() {
        assertParseFails("""{"type":"spacer","height":-1}""")
    }

    @Test
    fun unknownEnumTokenFails() {
        assertParseFails("""{"type":"text","text":"x","overflow":"fade"}""")
    }
}
