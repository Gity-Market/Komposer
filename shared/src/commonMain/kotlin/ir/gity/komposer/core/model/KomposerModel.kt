package ir.gity.komposer.core.model

import ir.gity.komposer.core.model.modifier.KomposerModifier

// commonMain — no Compose, no widget, no java.* imports allowed in this layer.
interface KomposerModel {
    // The ordered modifier list attaches to every node uniformly (SPEC-0005 §1), enforced at
    // the type level: every implementor overrides this, so "every node accepts modifiers" is a
    // compiler promise, not a review one. Defaults to empty; "modifiers": [] == absent.
    val modifiers: List<KomposerModifier>

    fun accept(visitor: KomposerModelVisitor)
}
