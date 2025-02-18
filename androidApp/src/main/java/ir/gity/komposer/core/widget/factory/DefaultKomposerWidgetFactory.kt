package ir.gity.komposer.core.widget.factory

import androidx.compose.runtime.Composable
import ir.gity.komposer.core.KomposerWidget
import ir.gity.komposer.core.model.KomposerModel

class DefaultKomposerWidgetFactory constructor(
    val widgetFactories: Map<Class<out KomposerModel>, KomposerWidgetFactory>
) : KomposerWidgetFactory {
    override fun create(model: KomposerModel): KomposerWidget {
        val factory = widgetFactories[model::class.java] ?: throw IllegalArgumentException("No factory registered for ${model::class.java}")
        return factory.create(model)
    }

}
