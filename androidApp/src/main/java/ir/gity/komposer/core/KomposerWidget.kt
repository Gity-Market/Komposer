package ir.gity.komposer.core

import androidx.compose.runtime.Composable
import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.visitor.KomposerWidgetVisitor

// Element
interface KomposerWidget {
    fun toModel(): KomposerModel

    @Composable
    fun Accept(
        visitor: KomposerWidgetVisitor
    )
}