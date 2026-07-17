package ir.gity.komposer.core.serialization

import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.column.ColumnModel
import ir.gity.komposer.core.model.spacer.SpacerModel
import ir.gity.komposer.core.model.text.TextModel
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * The single wire-level registration point for node types (SPEC-0003 §3) — the wire half
 * of the "one place to register a node" goal; the render half is the factory registry.
 *
 * Registered against `KomposerModel::class`, not `Any::class`: registering against `Any`
 * never matches properties typed `KomposerModel`.
 */
object KomposerSchema {
    val module = SerializersModule {
        polymorphic(KomposerModel::class) {
            subclass(TextModel::class)
            subclass(ColumnModel::class)
            subclass(SpacerModel::class)
            // New node types register here.
        }
        // No `polymorphic(KomposerModifier::class)` block, deliberately (SPEC-0005 §4): the
        // modifier hierarchy is *sealed*, so kotlinx.serialization derives its closed
        // polymorphic serializer with the configured "type" discriminator automatically —
        // there is nothing to register. Do not "fix" this absence.
    }
}
