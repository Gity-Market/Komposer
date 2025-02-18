package ir.gity.komposer.core.base

import androidx.compose.ui.text.TextStyle
import ir.gity.komposer.core.KomposerWidget
import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.column.ColumnModel
import ir.gity.komposer.core.model.spacer.SpacerModel
import ir.gity.komposer.core.model.text.TextModel
import ir.gity.komposer.core.widget.factory.DefaultKomposerWidgetFactory
import ir.gity.komposer.core.widget.factory.KomposerWidgetFactory
import ir.gity.komposer.core.widget.text.TextWidget
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass


class FactoryRegistry {
    private val factories = mutableMapOf<Class<out KomposerModel>, KomposerWidgetFactory>()
    fun register(modelClass: Class<out KomposerModel>, factory: KomposerWidgetFactory) {
        factories[modelClass] = factory
    }

    fun build(): DefaultKomposerWidgetFactory = DefaultKomposerWidgetFactory(factories)
}

interface KomposerState {
    fun saveState(): KomposerModel
    fun restoreState(state: KomposerModel)
}

interface KomposerSerializer {
    fun <T : KomposerModel> serialize(model: T): String
    fun <T : KomposerModel> deserialize(jsonString: String, clazz: Class<T>): T
}

class DefaultKomposerSerializer(
) : KomposerSerializer {
    private val module = SerializersModule {
        polymorphic(Any::class) {
            subclass(TextModel::class)
            subclass(ColumnModel::class)
            subclass(SpacerModel::class)
        }
    }
    private val format = Json { serializersModule = module }

    override fun <T : KomposerModel> serialize(model: T): String {
        TODO("")
    }


    override fun <T : KomposerModel> deserialize(jsonString: String, clazz: Class<T>): T {
        TODO("")
    }
}

interface KomposerMapper {
    fun modelToWidget(model: KomposerModel): KomposerWidget
    fun widgetToModel(widget: KomposerWidget): KomposerModel
}

class DefaultKomposerMapper : KomposerMapper {
    override fun modelToWidget(model: KomposerModel): KomposerWidget {
        TODO("Not yet implemented")
    }

    override fun widgetToModel(widget: KomposerWidget): KomposerModel {
        TODO("Not yet implemented")
    }
}

class KomposerEngine(
    private val serializer: KomposerSerializer,
    private val mapper: KomposerMapper
) {
    fun renderJsonToWidget(json: String, clazz: Class<out KomposerModel>): KomposerWidget {
        val model = serializer.deserialize(json, clazz)
        return mapper.modelToWidget(model)
    }

    fun renderWidgetToJson(widget: KomposerWidget): String {
        val model = mapper.widgetToModel(widget)
        return serializer.serialize(model)
    }
}

/****************************************************/


interface Specification<T> {
    fun isSatisfiedBy(candidate: T): Boolean
}

class NonEmptyTextSpecification : Specification<TextModel> {
    override fun isSatisfiedBy(candidate: TextModel): Boolean {
        return !candidate.text.isNullOrBlank()
    }
}


fun createTextWidget(model: TextModel): TextWidget {
    val spec = NonEmptyTextSpecification()
    if (!spec.isSatisfiedBy(model)) {
        throw IllegalArgumentException("Text cannot be empty!")
    }
    return TextWidget(
        text = model.text ?: "",
        style = TextStyle.Default
    )
}


interface KomposerModelVisitor {
    fun visit(textModel: TextModel)
    fun visit(columnModel: ColumnModel)
    fun visit(spacerModel: SpacerModel)
}

interface KomposerJsonFactory {
    fun createFromJson(jsonString: String): KomposerWidget
}

class DefaultKomposerJsonFactory(
    private val serializer: KomposerSerializer,
    private val widgetFactories: Map<Class<out KomposerModel>, KomposerWidgetFactory>
) : KomposerJsonFactory {

    override fun createFromJson(jsonString: String): KomposerWidget {
        // Deserialize the JSON string into the model
        val model = serializer.deserialize(jsonString, KomposerModel::class.java)
        // Use the appropriate factory to create the widget
        val factory = widgetFactories[model::class.java]
            ?: throw IllegalArgumentException("No factory registered for ${model::class.java}")
        return factory.create(model)
    }
}


class KomposerWidgetMapper(
    private val widgetFactories: Map<Class<out KomposerModel>, KomposerWidgetFactory>
) {
    fun modelToWidget(model: KomposerModel): KomposerWidget {
        val factory = widgetFactories[model::class.java]
            ?: throw IllegalArgumentException("No factory registered for ${model::class.java}")
        return factory.create(model)
    }

    fun widgetToModel(widget: KomposerWidget): KomposerModel {
        // Reverse mapping logic if needed
        TODO()
    }
}
