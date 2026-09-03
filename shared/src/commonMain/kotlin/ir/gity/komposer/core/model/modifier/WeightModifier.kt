package ir.gity.komposer.core.model.modifier

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Maps to `ColumnScope.weight` (and `RowScope.weight` when Row arrives in Phase 4).
 * The field is `value`, not `weight` — `{"type":"weight","weight":1}` stutters.
 *
 * **Scoping is render-time, by design:** a model's `init` cannot see the tree it sits in, so
 * `weight` parses anywhere and **fails loudly at render** (`KomposerRenderException`) when
 * folded without an enclosing weight-capable scope.
 */
@Serializable
@SerialName("weight")
data class WeightModifier(
    val value: Float,
    val fill: Boolean? = null,
) : KomposerModifier {
    init { require(value.isFinite() && value > 0f) { "weight value must be > 0, was $value" } }
}
