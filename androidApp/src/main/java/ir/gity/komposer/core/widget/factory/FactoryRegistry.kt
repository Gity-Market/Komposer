package ir.gity.komposer.core.widget.factory

import ir.gity.komposer.core.model.KomposerModel
import kotlin.reflect.KClass

/**
 * The render-level half of the "one place to register a node" goal (SPEC-0004 §1). Keyed
 * by `KClass`, not `java.lang.Class`, so it can follow the registry into `commonMain`
 * later. `build()` snapshots the registrations so a later `register()` cannot mutate a
 * factory already handed out.
 */
class FactoryRegistry {
    private val factories = mutableMapOf<KClass<out KomposerModel>, KomposerWidgetFactory>()

    fun register(modelClass: KClass<out KomposerModel>, factory: KomposerWidgetFactory) {
        factories[modelClass] = factory
    }

    inline fun <reified T : KomposerModel> register(factory: KomposerWidgetFactory) =
        register(T::class, factory)

    fun build(): DefaultKomposerWidgetFactory = DefaultKomposerWidgetFactory(factories.toMap())
}
