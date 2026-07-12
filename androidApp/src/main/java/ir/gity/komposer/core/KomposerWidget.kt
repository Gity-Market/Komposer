package ir.gity.komposer.core

import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.visitor.KomposerWidgetVisitor

// Element
interface KomposerWidget {
    fun toModel(): KomposerModel

    // Not @Composable: traversal builds data (e.g. a debug graph), not UI, so it can run
    // anywhere — tests, background threads (SPEC-0004 §5).
    fun Accept(visitor: KomposerWidgetVisitor)
}
