package ir.gity.komposer.core.model.column

import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.KomposerModelVisitor
import ir.gity.komposer.core.model.layout.HorizontalAlignmentValue
import ir.gity.komposer.core.model.layout.VerticalArrangementValue
import ir.gity.komposer.core.model.modifier.KomposerModifier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Maps to `androidx.compose.foundation.layout.Column` (SPEC-0002 §2, SPEC-0005 §3).
 *
 * Plain `List<KomposerModel>` — interface-typed properties serialize polymorphically via
 * the registered module; the old `@Contextual` annotation actively routed them away from
 * that and is removed (SPEC-0003 §3).
 *
 * `verticalArrangement` / `horizontalAlignment` are **node fields**, not modifiers: in
 * Compose they are `Column` parameters, not `Modifier` calls (SPEC-0005 §3). `modifiers` is
 * the last constructor parameter so positional construction sites keep compiling.
 */
@Serializable
@SerialName("column")
data class ColumnModel(
    val children: List<KomposerModel> = emptyList(),
    val verticalArrangement: VerticalArrangementValue? = null,
    val horizontalAlignment: HorizontalAlignmentValue? = null,
    override val modifiers: List<KomposerModifier> = emptyList(),
) : KomposerModel {
    override fun accept(visitor: KomposerModelVisitor) = visitor.visit(this)
}
