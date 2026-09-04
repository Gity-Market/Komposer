package ir.gity.komposer.core.serialization

import ir.gity.komposer.core.model.BoxModel
import ir.gity.komposer.core.model.ColumnModel
import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.RowModel
import ir.gity.komposer.core.model.TextModel
import ir.gity.komposer.core.model.layout.AlignmentValue
import ir.gity.komposer.core.model.layout.HorizontalAlignmentValue
import ir.gity.komposer.core.model.layout.HorizontalArrangementValue
import ir.gity.komposer.core.model.layout.VerticalAlignmentValue
import ir.gity.komposer.core.model.modifier.FillMaxWidthModifier
import ir.gity.komposer.core.model.modifier.PaddingModifier
import ir.gity.komposer.core.model.modifier.WeightModifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** JSON ⇄ model round-trips for the Phase 4 catalog nodes (`row`, `box`, …) and the `spacing` field. */
class CatalogRoundTripTest {

    private val serializer = DefaultKomposerSerializer()

    private fun roundTrip(model: KomposerModel): KomposerModel =
        serializer.parseNode(serializer.encodeNode(model))

    // --- row ---

    @Test
    fun rowLayoutFieldsRoundTripForEveryToken() {
        for (arrangement in HorizontalArrangementValue.entries) {
            val model = RowModel(horizontalArrangement = arrangement)
            assertEquals(model, roundTrip(model), "horizontalArrangement=$arrangement")
        }
        for (alignment in VerticalAlignmentValue.entries) {
            val model = RowModel(verticalAlignment = alignment)
            assertEquals(model, roundTrip(model), "verticalAlignment=$alignment")
        }
        // Both fields together.
        val both = RowModel(
            horizontalArrangement = HorizontalArrangementValue.SpaceEvenly,
            verticalAlignment = VerticalAlignmentValue.Bottom,
        )
        assertEquals(both, roundTrip(both))
    }

    @Test
    fun fullyPopulatedRowRoundTrips() {
        // `spacing` and `horizontalArrangement` are mutually exclusive, so "everything set" is two
        // variants: the arrangement token, and the gap.
        val withToken = RowModel(
            children = listOf(
                TextModel(text = "a", modifiers = listOf(WeightModifier(value = 1f))),
                TextModel(text = "b"),
            ),
            horizontalArrangement = HorizontalArrangementValue.SpaceBetween,
            verticalAlignment = VerticalAlignmentValue.Center,
            modifiers = listOf(FillMaxWidthModifier(), PaddingModifier(all = 8f)),
        )
        val withGap = withToken.copy(horizontalArrangement = null, spacing = 8f)
        assertEquals(withToken, roundTrip(withToken))
        assertEquals(withGap, roundTrip(withGap))
    }

    // --- box ---

    @Test
    fun boxContentAlignmentRoundTripsForEveryToken() {
        for (alignment in AlignmentValue.entries) {
            val model = BoxModel(contentAlignment = alignment)
            assertEquals(model, roundTrip(model), "contentAlignment=$alignment")
        }
        val populated = BoxModel(
            children = listOf(TextModel(text = "under"), TextModel(text = "over")),
            contentAlignment = AlignmentValue.BottomEnd,
            modifiers = listOf(FillMaxWidthModifier(), PaddingModifier(all = 8f)),
        )
        assertEquals(populated, roundTrip(populated))
    }

    // --- spacing, on both containers ---

    @Test
    fun spacingRoundTripsExactlyOnBothContainers() {
        for (spacing in listOf(0f, 8f, 12.5f)) {
            val row = RowModel(spacing = spacing, verticalAlignment = VerticalAlignmentValue.Center)
            val column = ColumnModel(spacing = spacing, horizontalAlignment = HorizontalAlignmentValue.End)
            assertEquals(row, roundTrip(row), "row spacing=$spacing")
            assertEquals(column, roundTrip(column), "column spacing=$spacing")
        }
    }

    @Test
    fun spacingEncodesAsPlainDpNumber() {
        assertEquals("""{"type":"row","spacing":8.0}""", serializer.encodeNode(RowModel(spacing = 8f)))
        assertEquals("""{"type":"column","spacing":8.0}""", serializer.encodeNode(ColumnModel(spacing = 8f)))
        // Absent spacing is omitted, like every other optional field.
        assertFalse("spacing" in serializer.encodeNode(RowModel()))
        assertFalse("spacing" in serializer.encodeNode(ColumnModel()))
    }

    // --- nesting across container types ---

    @Test
    fun mixedContainersPreserveOrderAndDepth() {
        val model = RowModel(
            spacing = 4f,
            children = listOf(
                TextModel(text = "a"),
                ColumnModel(
                    spacing = 2f,
                    children = listOf(
                        TextModel(text = "b"),
                        RowModel(
                            horizontalArrangement = HorizontalArrangementValue.End,
                            children = listOf(TextModel(text = "c"), TextModel(text = "d")),
                        ),
                        BoxModel(
                            contentAlignment = AlignmentValue.Center,
                            children = listOf(TextModel(text = "e"), RowModel(children = listOf(TextModel(text = "f")))),
                        ),
                    ),
                ),
                TextModel(text = "g"),
            ),
        )
        assertEquals(model, roundTrip(model))
    }
}
