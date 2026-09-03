package ir.gity.komposer.core.serialization

import ir.gity.komposer.core.model.KomposerDocument
import ir.gity.komposer.core.model.KomposerModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

class DefaultKomposerSerializer : KomposerSerializer {

    // No `serializersModule`: `KomposerModel` is a @Serializable sealed interface, so the
    // plugin-generated closed serializer enumerates the node catalog.
    private val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    // Nodes must go through the sealed base serializer: encoding a node via its concrete class
    // serializer writes no `type` discriminator, silently producing JSON that can never be
    // parsed back (the rule outlives the PolymorphicSerializer it was written about).
    private val nodeSerializer = serializer<KomposerModel>()

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
