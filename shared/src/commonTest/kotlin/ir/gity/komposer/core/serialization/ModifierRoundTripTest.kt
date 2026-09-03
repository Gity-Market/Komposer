package ir.gity.komposer.core.serialization

import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.ColumnModel
import ir.gity.komposer.core.model.layout.HorizontalAlignmentValue
import ir.gity.komposer.core.model.layout.VerticalArrangementValue
import ir.gity.komposer.core.model.modifier.BackgroundModifier
import ir.gity.komposer.core.model.modifier.FillMaxHeightModifier
import ir.gity.komposer.core.model.modifier.FillMaxSizeModifier
import ir.gity.komposer.core.model.modifier.FillMaxWidthModifier
import ir.gity.komposer.core.model.modifier.KomposerModifier
import ir.gity.komposer.core.model.modifier.PaddingModifier
import ir.gity.komposer.core.model.modifier.SizeModifier
import ir.gity.komposer.core.model.modifier.WeightModifier
import ir.gity.komposer.core.model.SpacerModel
import ir.gity.komposer.core.model.TextModel
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ModifierRoundTripTest {

    private val serializer = DefaultKomposerSerializer()

    private fun roundTrip(model: KomposerModel): KomposerModel =
        serializer.parseNode(serializer.encodeNode(model))

    /** Round-trips a modifier by attaching it to a carrier node. */
    private fun assertModifierRoundTrips(modifier: KomposerModifier) {
        val carrier = TextModel(text = "carrier", modifiers = listOf(modifier))
        assertEquals(carrier, roundTrip(carrier))
    }

    @Test
    fun everyModifierTypeRoundTripsMinimalAndPopulated() {
        val modifiers = listOf(
            // padding — one per group
            PaddingModifier(all = 8f),
            PaddingModifier(horizontal = 12f, vertical = 4f),
            PaddingModifier(start = 1f, top = 2f, end = 3f, bottom = 4f),
            // size — each shape
            SizeModifier(width = 100f),
            SizeModifier(height = 50f),
            SizeModifier(width = 100f, height = 50f),
            // fill — minimal (default fraction) + populated
            FillMaxWidthModifier(),
            FillMaxWidthModifier(fraction = 0.5f),
            FillMaxHeightModifier(),
            FillMaxHeightModifier(fraction = 0.25f),
            FillMaxSizeModifier(),
            FillMaxSizeModifier(fraction = 1f),
            // background — 6 and 8 digit
            BackgroundModifier(color = "#FFD54F"),
            BackgroundModifier(color = "#80FFD54F"),
            // weight — minimal + populated
            WeightModifier(value = 1f),
            WeightModifier(value = 2f, fill = false),
        )
        for (modifier in modifiers) {
            assertModifierRoundTrips(modifier)
        }
    }

    @Test
    fun orderAndRepetitionSurviveRoundTrip() {
        val model = TextModel(
            text = "chip",
            modifiers = listOf(
                PaddingModifier(all = 8f),
                BackgroundModifier(color = "#FFD54F"),
                PaddingModifier(all = 4f),
            ),
        )
        val result = roundTrip(model) as TextModel
        assertEquals(model, result)
        // Order and the duplicate padding are both intact.
        assertEquals(3, result.modifiers.size)
        assertEquals(PaddingModifier(all = 8f), result.modifiers[0])
        assertEquals(BackgroundModifier(color = "#FFD54F"), result.modifiers[1])
        assertEquals(PaddingModifier(all = 4f), result.modifiers[2])
    }

    @Test
    fun crossGroupPaddingSpellingsAreDistinctAndBothRoundTrip() {
        // Render identically, but are distinct models and both round-trip exactly.
        val horizontal = TextModel(text = "x", modifiers = listOf(PaddingModifier(horizontal = 8f)))
        val edges = TextModel(text = "x", modifiers = listOf(PaddingModifier(start = 8f, end = 8f)))
        assertFalse(horizontal == edges)
        assertEquals(horizontal, roundTrip(horizontal))
        assertEquals(edges, roundTrip(edges))
    }

    @Test
    fun columnLayoutFieldsRoundTripForEveryToken() {
        for (arrangement in VerticalArrangementValue.entries) {
            val model = ColumnModel(verticalArrangement = arrangement)
            assertEquals(model, roundTrip(model), "verticalArrangement=$arrangement")
        }
        for (alignment in HorizontalAlignmentValue.entries) {
            val model = ColumnModel(horizontalAlignment = alignment)
            assertEquals(model, roundTrip(model), "horizontalAlignment=$alignment")
        }
        // Both fields together.
        val both = ColumnModel(
            verticalArrangement = VerticalArrangementValue.SpaceBetween,
            horizontalAlignment = HorizontalAlignmentValue.End,
        )
        assertEquals(both, roundTrip(both))
    }

    @Test
    fun emptyModifierListIsOmittedFromEncoding() {
        val encoded = serializer.encodeNode(SpacerModel(height = 8f))
        assertFalse("modifiers" in encoded, "empty modifiers must be omitted: $encoded")
    }

    @Test
    fun defaultModifierFieldsAreOmittedFromEncoding() {
        // fraction defaults to null → omitted; fill defaults to null → omitted.
        val encoded = serializer.encodeNode(
            TextModel(
                text = "x",
                modifiers = listOf(FillMaxWidthModifier(), WeightModifier(value = 1f)),
            ),
        )
        assertContains(encoded, """{"type":"fillMaxWidth"}""")
        assertContains(encoded, """{"type":"weight","value":1.0}""")
        // Check for the JSON *keys* (with colon): "fill" alone is a substring of "fillMaxWidth".
        assertFalse("\"fraction\":" in encoded)
        assertFalse("\"fill\":" in encoded)
    }
}
