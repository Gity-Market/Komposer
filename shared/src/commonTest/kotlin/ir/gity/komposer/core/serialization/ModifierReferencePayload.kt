package ir.gity.komposer.core.serialization

import ir.gity.komposer.core.model.KomposerDocument
import ir.gity.komposer.core.model.ColumnModel
import ir.gity.komposer.core.model.layout.HorizontalAlignmentValue
import ir.gity.komposer.core.model.layout.VerticalArrangementValue
import ir.gity.komposer.core.model.modifier.BackgroundModifier
import ir.gity.komposer.core.model.modifier.FillMaxSizeModifier
import ir.gity.komposer.core.model.modifier.FillMaxWidthModifier
import ir.gity.komposer.core.model.modifier.PaddingModifier
import ir.gity.komposer.core.model.modifier.SizeModifier
import ir.gity.komposer.core.model.modifier.WeightModifier
import ir.gity.komposer.core.model.SpacerModel
import ir.gity.komposer.core.model.FontStyleValue
import ir.gity.komposer.core.model.TextModel
import ir.gity.komposer.core.model.TextOverflowValue

/** Modifier reference payload — the Phase 3 acceptance surface, shared across tests. */
val MODIFIER_REFERENCE_JSON = """
{
  "version": 1,
  "root": {
    "type": "column",
    "modifiers": [
      { "type": "fillMaxSize" },
      { "type": "background", "color": "#F2F2F7" },
      { "type": "padding", "all": 16 }
    ],
    "horizontalAlignment": "center",
    "children": [
      { "type": "text", "text": "Hello Komposer, modified", "fontWeight": 700, "fontSize": 20, "color": "#6200EE" },
      { "type": "spacer", "height": 12 },
      {
        "type": "text",
        "text": "background → padding: the yellow includes this inset",
        "modifiers": [
          { "type": "background", "color": "#FFD54F" },
          { "type": "padding", "horizontal": 12, "vertical": 4 }
        ]
      },
      {
        "type": "text",
        "text": "padding → background: the yellow hugs the text",
        "modifiers": [
          { "type": "padding", "horizontal": 12, "vertical": 4 },
          { "type": "background", "color": "#FFD54F" }
        ]
      },
      { "type": "spacer", "height": 12 },
      {
        "type": "text",
        "text": "weighted: fills the leftover vertical space",
        "modifiers": [
          { "type": "weight", "value": 1 },
          { "type": "fillMaxWidth" },
          { "type": "background", "color": "#E1F5FE" }
        ]
      },
      {
        "type": "column",
        "modifiers": [
          { "type": "fillMaxWidth", "fraction": 0.5 },
          { "type": "size", "height": 120 },
          { "type": "background", "color": "#EDE7F6" },
          { "type": "padding", "all": 8 }
        ],
        "verticalArrangement": "spaceBetween",
        "children": [
          { "type": "text", "text": "half width, 120dp tall", "fontStyle": "italic" },
          { "type": "text", "text": "spaceBetween pushes me down", "maxLines": 1, "overflow": "ellipsis" }
        ]
      }
    ]
  }
}
""".trimIndent()

val MODIFIER_REFERENCE_DOCUMENT = KomposerDocument(
    version = 1,
    root = ColumnModel(
        modifiers = listOf(
            FillMaxSizeModifier(),
            BackgroundModifier("#F2F2F7"),
            PaddingModifier(all = 16f),
        ),
        horizontalAlignment = HorizontalAlignmentValue.Center,
        children = listOf(
            TextModel(
                text = "Hello Komposer, modified",
                fontWeight = 700,
                fontSize = 20f,
                color = "#6200EE",
            ),
            SpacerModel(height = 12f),
            TextModel(
                text = "background → padding: the yellow includes this inset",
                modifiers = listOf(
                    BackgroundModifier("#FFD54F"),
                    PaddingModifier(horizontal = 12f, vertical = 4f),
                ),
            ),
            TextModel(
                text = "padding → background: the yellow hugs the text",
                modifiers = listOf(
                    PaddingModifier(horizontal = 12f, vertical = 4f),
                    BackgroundModifier("#FFD54F"),
                ),
            ),
            SpacerModel(height = 12f),
            TextModel(
                text = "weighted: fills the leftover vertical space",
                modifiers = listOf(
                    WeightModifier(value = 1f),
                    FillMaxWidthModifier(),
                    BackgroundModifier("#E1F5FE"),
                ),
            ),
            ColumnModel(
                modifiers = listOf(
                    FillMaxWidthModifier(fraction = 0.5f),
                    SizeModifier(height = 120f),
                    BackgroundModifier("#EDE7F6"),
                    PaddingModifier(all = 8f),
                ),
                verticalArrangement = VerticalArrangementValue.SpaceBetween,
                children = listOf(
                    TextModel(text = "half width, 120dp tall", fontStyle = FontStyleValue.Italic),
                    TextModel(
                        text = "spaceBetween pushes me down",
                        maxLines = 1,
                        overflow = TextOverflowValue.Ellipsis,
                    ),
                ),
            ),
        ),
    ),
)
