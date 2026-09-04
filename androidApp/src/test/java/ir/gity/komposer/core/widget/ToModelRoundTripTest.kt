package ir.gity.komposer.core.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp
import ir.gity.komposer.core.model.ColumnModel
import ir.gity.komposer.core.model.FontStyleValue
import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.RowModel
import ir.gity.komposer.core.model.SpacerModel
import ir.gity.komposer.core.model.TextAlignValue
import ir.gity.komposer.core.model.TextDecorationValue
import ir.gity.komposer.core.model.TextModel
import ir.gity.komposer.core.model.TextOverflowValue
import ir.gity.komposer.core.model.layout.HorizontalAlignmentValue
import ir.gity.komposer.core.model.layout.HorizontalArrangementValue
import ir.gity.komposer.core.model.layout.VerticalAlignmentValue
import ir.gity.komposer.core.model.layout.VerticalArrangementValue
import ir.gity.komposer.core.model.modifier.BackgroundModifier
import ir.gity.komposer.core.model.modifier.FillMaxSizeModifier
import ir.gity.komposer.core.model.modifier.FillMaxWidthModifier
import ir.gity.komposer.core.model.modifier.PaddingModifier
import ir.gity.komposer.core.model.modifier.WeightModifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ToModelRoundTripTest {

    private fun roundTrip(model: KomposerModel): KomposerModel =
        model.toWidget().toModel()

    private val fullCanonicalText = TextModel(
        text = "everything",
        color = "#FF6200EE",
        fontSize = 18f,
        fontWeight = 600,
        fontStyle = FontStyleValue.Italic,
        letterSpacing = 0.5f,
        textDecoration = TextDecorationValue.Underline,
        textAlign = TextAlignValue.Center,
        lineHeight = 24f,
        overflow = TextOverflowValue.Ellipsis,
        softWrap = false,
        maxLines = 3,
        minLines = 2,
    )

    @Test
    fun canonicalModelsRoundTripExactly() {
        val canonical = listOf(
            TextModel(text = "hi"),
            fullCanonicalText,
            SpacerModel(height = 16f),
            ColumnModel(
                children = listOf(
                    fullCanonicalText,
                    SpacerModel(height = 8f),
                    ColumnModel(children = listOf(TextModel(text = "nested"))),
                ),
            ),
            RowModel(
                children = listOf(
                    fullCanonicalText,
                    RowModel(children = listOf(TextModel(text = "nested"))),
                ),
            ),
        )
        for (model in canonical) {
            assertEquals(model, roundTrip(model), "canonical round-trip failed for $model")
        }
    }

    @Test
    fun nonCanonicalNormalizesToAbsentDefaults() {
        val nonCanonical = TextModel(
            text = "hi",
            overflow = TextOverflowValue.Clip,   // == Compose default → normalizes to null
            softWrap = true,                      // == default → null
            maxLines = Int.MAX_VALUE,             // == default → null
            minLines = 1,                         // == default → null
        )
        assertEquals(TextModel(text = "hi"), roundTrip(nonCanonical))
    }

    @Test
    fun normalizationIsIdempotent() {
        val nonCanonical = TextModel(text = "hi", overflow = TextOverflowValue.Clip, softWrap = true)
        val once = roundTrip(nonCanonical)
        val twice = roundTrip(once)
        assertEquals(once, twice)
    }

    @Test
    fun sixDigitColorComesBackAsEightDigitUppercase() {
        val model = TextModel(text = "x", color = "#6200EE")
        assertEquals(TextModel(text = "x", color = "#FF6200EE"), roundTrip(model))
    }

    // --- Modifiers survive exactly; column layout fields normalize ---

    @Test
    fun arbitraryModifierListsSurviveRoundTripExactly() {
        // Widgets store the model list verbatim, so modifiers are exact for
        // *every* list — including one with modifiers on the composite and its children.
        val model = ColumnModel(
            modifiers = listOf(
                FillMaxSizeModifier(),
                BackgroundModifier("#F2F2F7"),
                PaddingModifier(all = 16f),
            ),
            children = listOf(
                TextModel(
                    text = "a",
                    modifiers = listOf(
                        BackgroundModifier("#FFD54F"),
                        PaddingModifier(horizontal = 12f, vertical = 4f),
                    ),
                ),
                SpacerModel(height = 8f, modifiers = listOf(WeightModifier(value = 1f))),
            ),
        )
        assertEquals(model, roundTrip(model))
    }

    @Test
    fun explicitModifierDefaultsAreNotNormalized() {
        // Unlike column layout fields, modifiers are stored verbatim: explicit client-side
        // defaults (fraction = 1, fill = true) survive rather than collapsing.
        val model = TextModel(
            text = "x",
            modifiers = listOf(
                FillMaxWidthModifier(fraction = 1f),
                WeightModifier(value = 1f, fill = true),
            ),
        )
        assertEquals(model, roundTrip(model))
    }

    @Test
    fun canonicalColumnLayoutFieldsRoundTrip() {
        val model = ColumnModel(
            verticalArrangement = VerticalArrangementValue.SpaceBetween,
            horizontalAlignment = HorizontalAlignmentValue.Center,
        )
        assertEquals(model, roundTrip(model))
    }

    @Test
    fun explicitDefaultColumnLayoutNormalizesToAbsent() {
        // Top / Start equal the Compose defaults → normalize back to absent.
        val nonCanonical = ColumnModel(
            verticalArrangement = VerticalArrangementValue.Top,
            horizontalAlignment = HorizontalAlignmentValue.Start,
        )
        assertEquals(ColumnModel(), roundTrip(nonCanonical))
    }

    @Test
    fun columnLayoutNormalizationIsIdempotent() {
        val nonCanonical = ColumnModel(verticalArrangement = VerticalArrangementValue.Top)
        val once = roundTrip(nonCanonical)
        val twice = roundTrip(once)
        assertEquals(once, twice)
    }

    // --- Row: the same three regimes on the other axis ---

    @Test
    fun canonicalRowLayoutFieldsRoundTrip() {
        for (arrangement in HorizontalArrangementValue.entries.filter { it != HorizontalArrangementValue.Start }) {
            val model = RowModel(horizontalArrangement = arrangement)
            assertEquals(model, roundTrip(model), "horizontalArrangement=$arrangement")
        }
        for (alignment in VerticalAlignmentValue.entries.filter { it != VerticalAlignmentValue.Top }) {
            val model = RowModel(verticalAlignment = alignment)
            assertEquals(model, roundTrip(model), "verticalAlignment=$alignment")
        }
        val both = RowModel(
            horizontalArrangement = HorizontalArrangementValue.SpaceAround,
            verticalAlignment = VerticalAlignmentValue.Bottom,
            modifiers = listOf(FillMaxWidthModifier()),
        )
        assertEquals(both, roundTrip(both))
    }

    @Test
    fun explicitDefaultRowLayoutNormalizesToAbsent() {
        // Start / Top equal the Compose defaults → normalize back to absent.
        val nonCanonical = RowModel(
            horizontalArrangement = HorizontalArrangementValue.Start,
            verticalAlignment = VerticalAlignmentValue.Top,
        )
        assertEquals(RowModel(), roundTrip(nonCanonical))
    }

    @Test
    fun rowLayoutNormalizationIsIdempotent() {
        val nonCanonical = RowModel(horizontalArrangement = HorizontalArrangementValue.Start)
        val once = roundTrip(nonCanonical)
        val twice = roundTrip(once)
        assertEquals(once, twice)
    }

    // --- spacing: stored as its own Dp?, so exact for every value (including 0) ---

    @Test
    fun spacingSurvivesExactlyOnBothContainers() {
        for (spacing in listOf(0f, 8f, 12.5f)) {
            val row = RowModel(spacing = spacing, verticalAlignment = VerticalAlignmentValue.Center)
            val column = ColumnModel(spacing = spacing, horizontalAlignment = HorizontalAlignmentValue.End)
            assertEquals(row, roundTrip(row), "row spacing=$spacing")
            assertEquals(column, roundTrip(column), "column spacing=$spacing")
        }
    }

    @Test
    fun handBuiltWidgetRejectsSpacingWithArrangement() {
        // The widget mirrors the model's rule, so a contradiction cannot reach toModel().
        assertFailsWith<IllegalArgumentException> {
            RowWidget(horizontalArrangement = Arrangement.Center, spacing = 8.dp)
        }
        assertFailsWith<IllegalArgumentException> {
            ColumnWidget(verticalArrangement = Arrangement.Center, spacing = 8.dp)
        }
    }

    @Test
    fun mixedNestedTreeWithModifiersRoundTripsExactly() {
        val model = ColumnModel(
            spacing = 12f,
            modifiers = listOf(FillMaxSizeModifier(), PaddingModifier(all = 16f)),
            children = listOf(
                RowModel(
                    spacing = 8f,
                    verticalAlignment = VerticalAlignmentValue.Center,
                    modifiers = listOf(FillMaxWidthModifier()),
                    children = listOf(
                        TextModel(text = "weighted", modifiers = listOf(WeightModifier(value = 1f))),
                        TextModel(text = "fixed", fontStyle = FontStyleValue.Italic),
                        ColumnModel(
                            modifiers = listOf(PaddingModifier(all = 4f)),
                            children = listOf(TextModel(text = "nested", fontSize = 12f)),
                        ),
                    ),
                ),
                RowModel(
                    horizontalArrangement = HorizontalArrangementValue.SpaceBetween,
                    children = listOf(TextModel(text = "left"), TextModel(text = "right")),
                ),
            ),
        )
        assertEquals(model, roundTrip(model))
    }
}
