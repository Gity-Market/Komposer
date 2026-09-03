package ir.gity.komposer.core.model

import ir.gity.komposer.core.model.modifier.KomposerModifier
import kotlinx.serialization.Serializable

/**
 * The sealed base for the v1 node catalog.
 *
 * commonMain — no Compose, no widget, no `java.*` imports allowed in this layer.
 *
 * `@Serializable` on the sealed base is what makes the plugin emit the closed
 * `SealedClassSerializer` enumerating the subclasses, so a property typed `KomposerModel`
 * (`KomposerDocument.root`, `ColumnModel.children`) serializes with **closed** polymorphism and
 * the configured `"type"` discriminator, with no serializers-module registration — the same
 * mechanism `KomposerModifier` already proves.
 *
 * **Sealed, deliberately:** sealing *is* the registration. The old manual `polymorphic(KomposerModel::class)`
 * registration object and the model visitor existed only because this hierarchy was open; both
 * are deleted. Kotlin's same-package rule for sealed implementors is
 * why every node model lives in this package. Unknown node `type` on the wire ⇒
 * `SerializationException` ⇒ wrapped into `KomposerParseException` — the wire
 * strictness contract, unchanged.
 */
@Serializable
sealed interface KomposerModel {
    // The ordered modifier list attaches to every node uniformly, enforced at
    // the type level: every implementor overrides this, so "every node accepts modifiers" is a
    // compiler promise, not a review one. Defaults to empty; "modifiers": [] == absent.
    val modifiers: List<KomposerModifier>
}
