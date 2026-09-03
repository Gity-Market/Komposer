package ir.gity.komposer.core.widget

import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.modifier.KomposerModifier

/**
 * The Compose-aware half of a node: Compose-typed storage with Compose defaults
 * applied at mapping time, and `toModel()` to normalize back.
 *
 * **Sealed, not `@Serializable`** — widgets never touch the wire. Sealing buys the exhaustive
 * `when`s that replaced the visitor and the factory registry: renderer dispatch,
 * `toWidget()` and `debugGraph()` all become compile errors when a node is added without them.
 */
sealed interface KomposerWidget {
    // Widgets store the model modifier list verbatim; the render-time fold
    // turns it into a real Compose Modifier. Storing the list (not a folded Modifier) keeps
    // `toModel()` an identity for modifiers, so the round-trip is exact for every list.
    val modifiers: List<KomposerModifier>

    fun toModel(): KomposerModel
}
