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
import ir.gity.komposer.core.KomposerWidget
import ir.gity.komposer.core.model.column.ColumnModel
import ir.gity.komposer.core.model.spacer.SpacerModel
import ir.gity.komposer.core.model.text.TextModel
import ir.gity.komposer.core.renderer.KomposerRenderer
import ir.gity.komposer.core.serialization.DefaultKomposerSerializer
import ir.gity.komposer.core.visitor.GraphBuilder
import ir.gity.komposer.core.widget.factory.ColumnWidgetFactory
import ir.gity.komposer.core.widget.factory.FactoryRegistry
import ir.gity.komposer.core.widget.factory.SpacerWidgetFactory
import ir.gity.komposer.core.widget.factory.TextWidgetFactory

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

/** Registers the v1 catalog. No `Density` needed — works outside a composition. */
private fun v1Registry(): FactoryRegistry = FactoryRegistry().apply {
    register<ColumnModel>(ColumnWidgetFactory())
    register<TextModel>(TextWidgetFactory())
    register<SpacerModel>(SpacerWidgetFactory())
    // هر تعداد ویجت جدیدی که اضافه شد، فقط همینجا اضافه می‌شه
}

@Composable
private fun KomposerJson2ModelDemo() {
    // remember: deserialization + widget construction must not re-run on recomposition.
    val widget = remember {
        val document = DefaultKomposerSerializer().parse(REFERENCE_JSON)
        v1Registry().build().create(document.root)
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
        v1Registry().build().create(model)
    }
    val graph = remember(widget) { GraphBuilder().apply { Visit(widget) }.build() }
    SideEffect { Log.i(TAG, "KomposerModelDemo: $graph") }
    KomposerRenderer(widget = widget)
}

/** SPEC-0005 §9 reference payload — the Phase 3 acceptance surface (exercises every v1
 *  modifier except fillMaxHeight, both column layout fields, order sensitivity, weight scope). */
private val REFERENCE_JSON = """
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

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
        KomposerModelDemo()
    }
}

private const val TAG = "MainActivity"
