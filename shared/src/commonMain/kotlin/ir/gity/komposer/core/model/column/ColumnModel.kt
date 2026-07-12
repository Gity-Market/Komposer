package ir.gity.komposer.core.model.column

import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.KomposerModelVisitor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Maps to `androidx.compose.foundation.layout.Column` (SPEC-0002 §2).
 *
 * Plain `List<KomposerModel>` — interface-typed properties serialize polymorphically via
 * the registered module; the old `@Contextual` annotation actively routed them away from
 * that and is removed (SPEC-0003 §3).
 */
@Serializable
@SerialName("column")
data class ColumnModel(
    val children: List<KomposerModel> = emptyList(),
) : KomposerModel {
    override fun accept(visitor: KomposerModelVisitor) = visitor.visit(this)
}
