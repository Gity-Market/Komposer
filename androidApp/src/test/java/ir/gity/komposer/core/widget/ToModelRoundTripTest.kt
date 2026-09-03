package ir.gity.komposer.core.widget

import ir.gity.komposer.core.model.ColumnModel
import ir.gity.komposer.core.model.FontStyleValue
import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.SpacerModel
import ir.gity.komposer.core.model.TextAlignValue
import ir.gity.komposer.core.model.TextDecorationValue
import ir.gity.komposer.core.model.TextModel
import ir.gity.komposer.core.model.TextOverflowValue
import ir.gity.komposer.core.model.layout.HorizontalAlignmentValue
import ir.gity.komposer.core.model.layout.VerticalArrangementValue
import ir.gity.komposer.core.model.modifier.BackgroundModifier
import ir.gity.komposer.core.model.modifier.FillMaxSizeModifier
import ir.gity.komposer.core.model.modifier.FillMaxWidthModifier
import ir.gity.komposer.core.model.modifier.PaddingModifier
import ir.gity.komposer.core.model.modifier.WeightModifier
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
