package ir.gity.komposer.core.widget.factory

import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.column.ColumnModel
import ir.gity.komposer.core.model.spacer.SpacerModel
import ir.gity.komposer.core.model.text.FontStyleValue
import ir.gity.komposer.core.model.text.TextAlignValue
import ir.gity.komposer.core.model.text.TextDecorationValue
import ir.gity.komposer.core.model.text.TextModel
import ir.gity.komposer.core.model.text.TextOverflowValue
import kotlin.test.Test
import kotlin.test.assertEquals

class ToModelRoundTripTest {

    private val factory = FactoryRegistry().apply {
        register<ColumnModel>(ColumnWidgetFactory())
        register<TextModel>(TextWidgetFactory())
        register<SpacerModel>(SpacerWidgetFactory())
    }.build()

    private fun roundTrip(model: KomposerModel): KomposerModel =
        factory.create(model).toModel()

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
}
