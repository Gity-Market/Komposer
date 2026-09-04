package ir.gity.komposer.core.widget

import ir.gity.komposer.core.model.ColumnModel
import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.RowModel
import ir.gity.komposer.core.model.SpacerModel
import ir.gity.komposer.core.model.TextModel

/**
 * The single Model → Widget construction path, replacing the factory registry.
 *
 * Exhaustive by sealing — no `else`, deliberately: adding a node without a mapping branch is a
 * compile error. With exactly one construction path there is nothing to bypass, so the old
 * factory-bypass bug class (a composite building children without consulting the registry) is
 * structurally impossible rather than merely fixed.
 */
fun KomposerModel.toWidget(): KomposerWidget = when (this) {
    is TextModel -> toWidget()
    is ColumnModel -> toWidget()
    is RowModel -> toWidget()
    is SpacerModel -> toWidget()
}
