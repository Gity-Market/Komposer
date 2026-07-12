package ir.gity.komposer.core.model

import ir.gity.komposer.core.model.column.ColumnModel
import ir.gity.komposer.core.model.spacer.SpacerModel
import ir.gity.komposer.core.model.text.TextModel

/**
 * Dependency-free visitor over the model tree — usable server-side too (validation
 * passes, payload statistics) where Compose does not exist. Promoted out of the old
 * `NiceToHave.kt` sketch (SPEC-0003 §2).
 */
interface KomposerModelVisitor {
    fun visit(textModel: TextModel)
    fun visit(columnModel: ColumnModel)
    fun visit(spacerModel: SpacerModel)
}
