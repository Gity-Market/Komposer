package ir.gity.komposer.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ir.gity.komposer.Greeting
import ir.gity.komposer.core.KomposerWidget
import ir.gity.komposer.core.base.DefaultKomposerJsonFactory
import ir.gity.komposer.core.base.DefaultKomposerSerializer
import ir.gity.komposer.core.base.FactoryRegistry
import ir.gity.komposer.core.model.column.ColumnModel
import ir.gity.komposer.core.model.spacer.SpacerModel
import ir.gity.komposer.core.model.text.TextModel
import ir.gity.komposer.core.renderer.KomposerRenderer
import ir.gity.komposer.core.widget.factory.ColumnWidgetFactory
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
                    KomposerJson2ModelDemo()
                }
            }
        }
    }


}

@Composable
private fun KomposerModelDemo() {
    val model = ColumnModel(
        children = listOf(
            TextModel(text = "Hello World"),
            TextModel(text = "Hello World"),
            TextModel(text = "Hello World"),
            TextModel(text = "Hello World"),
            SpacerModel(
                px = LocalDensity.current.run { 16.dp.toPx() }
            ),
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
    val factoryRegistry = FactoryRegistry().apply {
        register(ColumnModel::class.java, ColumnWidgetFactory())
        register(TextModel::class.java, TextWidgetFactory())
        register(
            SpacerModel::class.java, SpacerWidgetFactory(
                density = LocalDensity.current
            )
        )
        // هر تعداد ویجت جدیدی که اضافه شد، فقط همینجا اضافه می‌شه
    }
    val widget: KomposerWidget = factoryRegistry.build().create(model)
    KomposerRenderer(widget = widget)
}

@Composable
private fun KomposerJson2ModelDemo() {
    val jsonString = """
    {
      "children": [
        {"text": "Hello Vahid"},
        {"text": "Hello World"},
        {"text": "Hello Vahid"},
        {"text": "Hello Vahid"},
        {"px": 16.0},
        {
          "children": [
            {"text": "Nested Hello Vahid"},
            {"text": "Nested Hello Vahid"}
          ]
        }
      ]
    }
    """

    val factoryRegistry = FactoryRegistry().apply {
        register(ColumnModel::class.java, ColumnWidgetFactory())
        register(TextModel::class.java, TextWidgetFactory())
        register(
            SpacerModel::class.java, SpacerWidgetFactory(
                density = LocalDensity.current
            )
        )
        // هر تعداد ویجت جدیدی که اضافه شد، فقط همینجا اضافه می‌شه
    }

    val jsonFactory = DefaultKomposerJsonFactory(
        serializer = DefaultKomposerSerializer(),
        widgetFactories = factoryRegistry.build().widgetFactories
    )

    val widget = jsonFactory.createFromJson(jsonString)
    KomposerRenderer(widget = widget)
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
        KomposerJson2ModelDemo()
    }
}
