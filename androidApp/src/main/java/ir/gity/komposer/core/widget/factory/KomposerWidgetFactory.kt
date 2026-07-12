package ir.gity.komposer.core.widget.factory

import ir.gity.komposer.core.KomposerWidget
import ir.gity.komposer.core.model.KomposerModel

interface KomposerWidgetFactory {
    /** [root] is the top-level dispatching factory; composites use it for their children. */
    fun create(model: KomposerModel, root: KomposerWidgetFactory): KomposerWidget
}
