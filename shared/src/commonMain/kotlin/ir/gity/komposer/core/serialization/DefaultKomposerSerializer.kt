package ir.gity.komposer.core.serialization

import ir.gity.komposer.core.model.KomposerDocument
import ir.gity.komposer.core.model.KomposerModel
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DefaultKomposerSerializer : KomposerSerializer {

    private val json = Json {
        serializersModule = KomposerSchema.module
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    // Nodes must go through the polymorphic serializer: encoding a node via its concrete
    // class serializer writes no `type` discriminator, silently producing JSON that can
    // never be parsed back (SPEC-0003 §5).
    private val nodeSerializer = PolymorphicSerializer(KomposerModel::class)

    override fun encode(document: KomposerDocument): String =
        wrap { json.encodeToString(document) }

    override fun parse(json: String): KomposerDocument =
        wrap { this.json.decodeFromString<KomposerDocument>(json) }

    override fun encodeNode(model: KomposerModel): String =
        wrap { json.encodeToString(nodeSerializer, model) }

    override fun parseNode(json: String): KomposerModel =
        wrap { this.json.decodeFromString(nodeSerializer, json) }

    // `SerializationException` is a documented subclass of `IllegalArgumentException`, and
    // model `init` validation throws `IllegalArgumentException` via `require` — so a single
    // catch covers both failure sources.
    private inline fun <T> wrap(block: () -> T): T =
        try {
            block()
        } catch (e: IllegalArgumentException) {
            throw KomposerParseException(e.message ?: "Invalid Komposer payload", e)
        }
}
