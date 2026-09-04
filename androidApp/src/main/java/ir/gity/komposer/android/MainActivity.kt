package ir.gity.komposer.android

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ir.gity.komposer.core.model.ColumnModel
import ir.gity.komposer.core.model.SpacerModel
import ir.gity.komposer.core.model.TextModel
import ir.gity.komposer.core.renderer.KomposerRenderer
import ir.gity.komposer.core.serialization.DefaultKomposerSerializer
import ir.gity.komposer.core.widget.KomposerWidget
import ir.gity.komposer.core.widget.debugGraph
import ir.gity.komposer.core.widget.toWidget

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // The JSON path is now the primary demo: raw JSON string → pixels.
                    KomposerJson2ModelDemo()
                }
            }
        }
    }
}

@Composable
private fun KomposerJson2ModelDemo() {
    // remember: deserialization + widget construction must not re-run on recomposition.
    val widget = remember {
        DefaultKomposerSerializer().parse(REFERENCE_JSON).root.toWidget()
    }
    KomposerRenderer(widget = widget)
}

@Composable
private fun KomposerModelDemo() {
    val widget: KomposerWidget = remember {
        val model = ColumnModel(
            children = listOf(
                TextModel(text = "Hello World"),
                TextModel(text = "Hello World"),
                TextModel(text = "Hello World"),
                TextModel(text = "Hello World"),
                SpacerModel(height = 16f),
                ColumnModel(
                    children = listOf(
                        TextModel(text = "Hello World"),
                        TextModel(text = "Hello World"),
                        TextModel(text = "Hello World"),
                        TextModel(text = "Hello World")
                    )
                )
            )
        )
        model.toWidget()
    }
    val graph = remember(widget) { widget.debugGraph() }
    SideEffect { Log.i(TAG, "KomposerModelDemo: $graph") }
    KomposerRenderer(widget = widget)
}

/** Catalog reference payload — the Phase 4 acceptance surface (mirrors `CATALOG_REFERENCE_JSON`
 *  in `commonTest`): `spacing` on both containers, every row layout field, `weight` inside a row,
 *  a column nested in a row, and a `box` stacking a cropped network `image` under an overlay. The
 *  Phase 3 modifier payload it replaces lives on as `MODIFIER_REFERENCE_JSON` in `commonTest`. */
private val REFERENCE_JSON = """
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

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
        KomposerModelDemo()
    }
}

private const val TAG = "MainActivity"
