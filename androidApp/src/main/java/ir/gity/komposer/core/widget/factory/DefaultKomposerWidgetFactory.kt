package ir.gity.komposer.core.widget.factory

import ir.gity.komposer.core.KomposerRenderException
import ir.gity.komposer.core.KomposerWidget
import ir.gity.komposer.core.model.KomposerModel
import kotlin.reflect.KClass

class DefaultKomposerWidgetFactory(
    private val factories: Map<KClass<out KomposerModel>, KomposerWidgetFactory>,
) : KomposerWidgetFactory {
    override fun create(model: KomposerModel, root: KomposerWidgetFactory): KomposerWidget {
        val factory = factories[model::class]
            ?: throw KomposerRenderException("No factory registered for ${model::class.simpleName}")
        return factory.create(model, root)
    }

    /** Entry point: dispatch through this factory as its own [root] so composites recurse. */
    fun create(model: KomposerModel): KomposerWidget = create(model, this)
}
