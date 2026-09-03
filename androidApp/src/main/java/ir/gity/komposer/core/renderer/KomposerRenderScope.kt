package ir.gity.komposer.core.renderer

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.Modifier

/**
 * `Modifier.weight` is a member of `ColumnScope`/`RowScope`, and those receivers exist only
 * inside the parent's content lambda at composition time. So the *parent's* renderer hands
 * its scope down one level, and a child folds `weight` through it.
 *
 * `null` means "no weight-capable parent" — folding a `weight` then fails loudly
 * (`KomposerRenderException`), the honest alternative to silently dropping UI.
 */
interface KomposerRenderScope {
    fun weight(modifier: Modifier, value: Float, fill: Boolean): Modifier
}

/** Adapts a Compose [ColumnScope] so column children can fold a `weight` modifier. */
class ColumnRenderScope(private val scope: ColumnScope) : KomposerRenderScope {
    override fun weight(modifier: Modifier, value: Float, fill: Boolean): Modifier =
        with(scope) { modifier.weight(value, fill) }
}
