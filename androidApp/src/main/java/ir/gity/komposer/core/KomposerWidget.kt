package ir.gity.komposer.core

import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.modifier.KomposerModifier
import ir.gity.komposer.core.visitor.KomposerWidgetVisitor

// Element
interface KomposerWidget {
    // Widgets store the model modifier list verbatim (SPEC-0005 §5.1); the render-time fold
    // turns it into a real Compose Modifier. Storing the list (not a folded Modifier) keeps
    // `toModel()` an identity for modifiers, so the round-trip is exact for every list.
    val modifiers: List<KomposerModifier>

    fun toModel(): KomposerModel

    // Not @Composable: traversal builds data (e.g. a debug graph), not UI, so it can run
    // anywhere — tests, background threads (SPEC-0004 §5).
    fun Accept(visitor: KomposerWidgetVisitor)
}
