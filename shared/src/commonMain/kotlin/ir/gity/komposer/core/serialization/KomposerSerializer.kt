package ir.gity.komposer.core.serialization

import ir.gity.komposer.core.model.KomposerDocument
import ir.gity.komposer.core.model.KomposerModel

/**
 * The wire (de)serializer. No `Class<T>` parameter: polymorphic parsing
 * makes a caller-supplied class unnecessary — the `type` discriminator decides.
 */
interface KomposerSerializer {
    fun encode(document: KomposerDocument): String
    fun parse(json: String): KomposerDocument
    fun encodeNode(model: KomposerModel): String
    fun parseNode(json: String): KomposerModel
}
