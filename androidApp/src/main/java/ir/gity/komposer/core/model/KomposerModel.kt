package ir.gity.komposer.core.model

import ir.gity.komposer.core.KomposerWidget
import ir.gity.komposer.core.base.KomposerModelVisitor


interface KomposerModel {
    fun toWidget(): KomposerWidget
    fun accept(visitor: KomposerModelVisitor)
}