package ir.gity.komposer.core.model

import ir.gity.komposer.core.model.layout.AlignmentValue
import ir.gity.komposer.core.model.modifier.KomposerModifier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Maps to `androidx.compose.foundation.layout.Box`: children stack in array order, later on top.
 *
 * `contentAlignment` is a **node field** (a `Box` parameter) carrying the two-dimensional
 * vocabulary — a box has no main axis, so neither per-axis enum fits. It positions *every*
 * child; per-child placement (`BoxScope.align`, which would be the first scoped modifier after
 * `weight`) is deferred.
 *
 * A box provides **no weight scope**: `weight` on a direct child parses (a model cannot see its
 * parent) and fails loudly at render, exactly like `weight` at the document root.
 *
 * `modifiers` is the last constructor parameter so positional construction sites keep compiling.
 */
@Serializable
@SerialName("box")
data class BoxModel(
    val children: List<KomposerModel> = emptyList(),
    val contentAlignment: AlignmentValue? = null,
    override val modifiers: List<KomposerModifier> = emptyList(),
) : KomposerModel
