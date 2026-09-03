package ir.gity.komposer.core.model.modifier

import kotlinx.serialization.Serializable

/**
 * The sealed base for the v1 modifier allow-list.
 *
 * commonMain — no Compose, no Android, no `java.*` imports.
 *
 * `@Serializable` on the sealed base is what makes the plugin emit the closed
 * `SealedClassSerializer` that enumerates the subclasses; a property typed `KomposerModifier`
 * then serializes with **closed** polymorphism and the configured `"type"` discriminator, with
 * no serializers-module registration. (Without the annotation kotlinx.serialization falls back
 * to *open* polymorphism and demands a `polymorphic(KomposerModifier)` module entry — the very
 * registration sealing exists to avoid: the annotation is required, the registration is not.)
 *
 * **Sealed, deliberately:** the only consumer is the render-time fold's `when`,
 * and sealing makes that `when` compiler-checked — adding a modifier without a fold branch is a
 * compile error. Sealing *is* the registration: nothing to register anywhere, no "known on the
 * wire but unknown to the fold" drift. `KomposerModel` follows the same pattern.
 * Unknown modifier `type` on the wire ⇒ `SerializationException` ⇒ wrapped into
 * `KomposerParseException` by the serializer machinery.
 */
@Serializable
sealed interface KomposerModifier
