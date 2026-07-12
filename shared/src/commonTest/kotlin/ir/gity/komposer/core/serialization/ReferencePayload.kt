package ir.gity.komposer.core.serialization

import ir.gity.komposer.core.model.KomposerDocument
import ir.gity.komposer.core.model.column.ColumnModel
import ir.gity.komposer.core.model.spacer.SpacerModel
import ir.gity.komposer.core.model.text.FontStyleValue
import ir.gity.komposer.core.model.text.TextModel
import ir.gity.komposer.core.model.text.TextOverflowValue

/** SPEC-0001 §7 reference payload, shared across tests. */
val REFERENCE_JSON = """
{
  "version": 1,
  "root": {
    "type": "column",
    "children": [
      { "type": "text", "text": "Hello Komposer", "fontWeight": 700, "fontSize": 20, "color": "#6200EE" },
      { "type": "text", "text": "One line only, ellipsized when it overflows the width", "maxLines": 1, "overflow": "ellipsis" },
      { "type": "spacer", "height": 16 },
      {
        "type": "column",
        "children": [
          { "type": "text", "text": "Nested, italic", "fontStyle": "italic" }
        ]
      }
    ]
  }
}
""".trimIndent()

val REFERENCE_DOCUMENT = KomposerDocument(
    version = 1,
    root = ColumnModel(
        children = listOf(
            TextModel(
                text = "Hello Komposer",
                fontWeight = 700,
                fontSize = 20f,
                color = "#6200EE",
            ),
            TextModel(
                text = "One line only, ellipsized when it overflows the width",
                maxLines = 1,
                overflow = TextOverflowValue.Ellipsis,
            ),
            SpacerModel(height = 16f),
            ColumnModel(
                children = listOf(
                    TextModel(text = "Nested, italic", fontStyle = FontStyleValue.Italic),
                ),
            ),
        ),
    ),
)
