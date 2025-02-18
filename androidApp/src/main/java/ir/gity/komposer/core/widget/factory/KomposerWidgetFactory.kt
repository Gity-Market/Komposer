package ir.gity.komposer.core.widget.factory

import ir.gity.komposer.core.KomposerWidget
import ir.gity.komposer.core.model.KomposerModel

interface KomposerWidgetFactory {
    fun create(model: KomposerModel): KomposerWidget
}