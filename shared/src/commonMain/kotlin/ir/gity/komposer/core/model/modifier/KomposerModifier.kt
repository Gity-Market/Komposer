package ir.gity.komposer.core.model.modifier

import kotlinx.serialization.Serializable

/**
 * The sealed base for the v1 modifier allow-list (SPEC-0005 §4).
 *
 * commonMain — no Compose, no Android, no `java.*` imports (SPEC-0003 §2 rules apply).
 *
 * `@Serializable` on the sealed base is what makes the plugin emit the closed
 * `SealedClassSerializer` that enumerates the subclasses; a property typed `KomposerModifier`
 * then serializes with **closed** polymorphism and the configured `"type"` discriminator, with
 * no `SerializersModule` registration. (Without the annotation kotlinx.serialization falls back
 * to *open* polymorphism and demands a `polymorphic(KomposerModifier)` module entry — the very
 * registration sealing exists to avoid. SPEC-0005 §4's prose predates this and is corrected in
 * the code: the annotation is required, the registration is not.)
 *
 * **Sealed, deliberately** (contrast `KomposerModel`, which is non-sealed and registered in
 * `KomposerSchema`): the only consumer is the render-time fold's `when` (SPEC-0005 §5.2), and
 * sealing makes that `when` compiler-checked — adding a modifier without a fold branch is a
 * compile error. Sealing *is* the registration: no `KomposerSchema` entry, no "registered on
 * the wire but unknown to the fold" drift. Unknown modifier `type` on the wire ⇒
 * `SerializationException` ⇒ wrapped into `KomposerParseException` by the SPEC-0003 §5 machinery.
 */
@Serializable
sealed interface KomposerModifier
