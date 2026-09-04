package ir.gity.komposer.core.serialization

import kotlin.test.Test
import kotlin.test.assertFailsWith

/** One failing-parse test per validation rule. */
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

    // --- image ---

    @Test
    fun blankImageUrlFails() {
        assertParseFails("""{"type":"image","url":""}""")
        assertParseFails("""{"type":"image","url":"   "}""")
    }

    // --- spacing (row + column) ---

    @Test
    fun negativeOrNonFiniteSpacingFails() {
        assertParseFails("""{"type":"row","spacing":-1}""")
        assertParseFails("""{"type":"column","spacing":-1}""")
        assertParseFails("""{"type":"row","spacing":"NaN"}""")
        assertParseFails("""{"type":"column","spacing":"Infinity"}""")
    }

    @Test
    fun spacingWithArrangementFails() {
        // spacing IS an arrangement: one slot, so the pair is a contradiction — even with the
        // default token spelled out.
        assertParseFails("""{"type":"row","spacing":8,"horizontalArrangement":"spaceBetween"}""")
        assertParseFails("""{"type":"row","spacing":8,"horizontalArrangement":"start"}""")
        assertParseFails("""{"type":"column","spacing":8,"verticalArrangement":"center"}""")
        assertParseFails("""{"type":"column","spacing":8,"verticalArrangement":"top"}""")
    }

    @Test
    fun zeroSpacingAndCrossAxisAlignmentWithSpacingAreLegal() {
        serializer.parseNode("""{"type":"row","spacing":0}""")
        serializer.parseNode("""{"type":"column","spacing":0}""")
        // Cross-axis alignment is a different slot; it combines freely with spacing.
        serializer.parseNode("""{"type":"row","spacing":8,"verticalAlignment":"center"}""")
        serializer.parseNode("""{"type":"column","spacing":8,"horizontalAlignment":"end"}""")
    }
}
