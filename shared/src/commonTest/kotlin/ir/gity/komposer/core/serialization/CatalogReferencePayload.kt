package ir.gity.komposer.core.serialization

import ir.gity.komposer.core.model.BoxModel
import ir.gity.komposer.core.model.ColumnModel
import ir.gity.komposer.core.model.ContentScaleValue
import ir.gity.komposer.core.model.FontStyleValue
import ir.gity.komposer.core.model.ImageModel
import ir.gity.komposer.core.model.KomposerDocument
import ir.gity.komposer.core.model.RowModel
import ir.gity.komposer.core.model.TextModel
import ir.gity.komposer.core.model.layout.AlignmentValue
import ir.gity.komposer.core.model.layout.HorizontalArrangementValue
import ir.gity.komposer.core.model.layout.VerticalAlignmentValue
import ir.gity.komposer.core.model.modifier.BackgroundModifier
import ir.gity.komposer.core.model.modifier.FillMaxSizeModifier
import ir.gity.komposer.core.model.modifier.FillMaxWidthModifier
import ir.gity.komposer.core.model.modifier.PaddingModifier
import ir.gity.komposer.core.model.modifier.SizeModifier
import ir.gity.komposer.core.model.modifier.WeightModifier

/**
 * Catalog reference payload — the Phase 4 acceptance surface, shared across tests and mirrored
 * by the on-device demo. Exercises: `spacing` on both containers, every row layout field,
 * `weight` inside a row, a column nested in a row, and a `box` stacking a cropped network
 * `image` under an overlay text at `bottomEnd`.
 */
val CATALOG_REFERENCE_JSON = """
{
  "version": 1,
  "root": {
    "type": "column",
    "modifiers": [ { "type": "fillMaxSize" }, { "type": "padding", "all": 16 } ],
    "spacing": 12,
    "children": [
      {
        "type": "row",
        "modifiers": [ { "type": "fillMaxWidth" } ],
        "verticalAlignment": "center",
        "spacing": 8,
        "children": [
          { "type": "text", "text": "8dp gaps", "fontWeight": 700 },
          { "type": "text", "text": "between" },
          { "type": "text", "text": "us" }
        ]
      },
      {
        "type": "row",
        "modifiers": [ { "type": "fillMaxWidth" } ],
        "horizontalArrangement": "spaceBetween",
        "children": [
          { "type": "text", "text": "far left" },
          { "type": "text", "text": "far right" }
        ]
      },
      {
        "type": "row",
        "modifiers": [ { "type": "fillMaxWidth" }, { "type": "background", "color": "#EDE7F6" } ],
        "verticalAlignment": "bottom",
        "children": [
          {
            "type": "text",
            "text": "weighted: I take the leftover width",
            "modifiers": [ { "type": "weight", "value": 1 } ]
          },
          { "type": "text", "text": "fixed", "fontStyle": "italic" },
          {
            "type": "column",
            "modifiers": [ { "type": "padding", "all": 4 } ],
            "children": [
              { "type": "text", "text": "nested", "fontSize": 12 },
              { "type": "text", "text": "column", "fontSize": 12 }
            ]
          }
        ]
      },
      {
        "type": "box",
        "modifiers": [ { "type": "size", "width": 200, "height": 120 } ],
        "contentAlignment": "bottomEnd",
        "children": [
          {
            "type": "image",
            "url": "https://picsum.photos/400/240",
            "contentDescription": "sample photo",
            "contentScale": "crop",
            "modifiers": [ { "type": "fillMaxSize" } ]
          },
          {
            "type": "text",
            "text": "overlay",
            "color": "#FFFFFF",
            "modifiers": [
              { "type": "background", "color": "#80000000" },
              { "type": "padding", "horizontal": 8, "vertical": 4 }
            ]
          }
        ]
      }
    ]
  }
}
""".trimIndent()

val CATALOG_REFERENCE_DOCUMENT = KomposerDocument(
    version = 1,
    root = ColumnModel(
        modifiers = listOf(FillMaxSizeModifier(), PaddingModifier(all = 16f)),
        spacing = 12f,
        children = listOf(
            RowModel(
                modifiers = listOf(FillMaxWidthModifier()),
                verticalAlignment = VerticalAlignmentValue.Center,
                spacing = 8f,
                children = listOf(
                    TextModel(text = "8dp gaps", fontWeight = 700),
                    TextModel(text = "between"),
                    TextModel(text = "us"),
                ),
            ),
            RowModel(
                modifiers = listOf(FillMaxWidthModifier()),
                horizontalArrangement = HorizontalArrangementValue.SpaceBetween,
                children = listOf(
                    TextModel(text = "far left"),
                    TextModel(text = "far right"),
                ),
            ),
            RowModel(
                modifiers = listOf(FillMaxWidthModifier(), BackgroundModifier("#EDE7F6")),
                verticalAlignment = VerticalAlignmentValue.Bottom,
                children = listOf(
                    TextModel(
                        text = "weighted: I take the leftover width",
                        modifiers = listOf(WeightModifier(value = 1f)),
                    ),
                    TextModel(text = "fixed", fontStyle = FontStyleValue.Italic),
                    ColumnModel(
                        modifiers = listOf(PaddingModifier(all = 4f)),
                        children = listOf(
                            TextModel(text = "nested", fontSize = 12f),
                            TextModel(text = "column", fontSize = 12f),
                        ),
                    ),
                ),
            ),
            BoxModel(
                modifiers = listOf(SizeModifier(width = 200f, height = 120f)),
                contentAlignment = AlignmentValue.BottomEnd,
                children = listOf(
                    ImageModel(
                        url = "https://picsum.photos/400/240",
                        contentDescription = "sample photo",
                        contentScale = ContentScaleValue.Crop,
                        modifiers = listOf(FillMaxSizeModifier()),
                    ),
                    TextModel(
                        text = "overlay",
                        color = "#FFFFFF",
                        modifiers = listOf(
                            BackgroundModifier("#80000000"),
                            PaddingModifier(horizontal = 8f, vertical = 4f),
                        ),
                    ),
                ),
            ),
        ),
    ),
)
