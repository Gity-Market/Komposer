package ir.gity.komposer.core.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.gity.komposer.core.KomposerRenderException
import ir.gity.komposer.core.model.modifier.BackgroundModifier
import ir.gity.komposer.core.model.modifier.FillMaxHeightModifier
import ir.gity.komposer.core.model.modifier.FillMaxSizeModifier
import ir.gity.komposer.core.model.modifier.FillMaxWidthModifier
import ir.gity.komposer.core.model.modifier.KomposerModifier
import ir.gity.komposer.core.model.modifier.PaddingModifier
import ir.gity.komposer.core.model.modifier.SizeModifier
import ir.gity.komposer.core.model.modifier.WeightModifier
import ir.gity.komposer.core.widget.factory.parseKomposerColor

/**
 * Folds an ordered modifier list into a real Compose [Modifier] (SPEC-0005 §5.2).
 *
 * A left fold ⇒ list order == chain order, the §1 guarantee: `[A, B, C]` → `Modifier.a().b().c()`.
 * There is **no `else` branch, deliberately** — `KomposerModifier` is sealed, so the `when` is
 * exhaustive and the compiler forces a branch for every new modifier type (§2.7).
 *
 * Pure and non-composable: it runs anywhere, tests included.
 */
fun List<KomposerModifier>.toComposeModifier(scope: KomposerRenderScope? = null): Modifier =
    fold<KomposerModifier, Modifier>(Modifier) { acc, modifier ->
        when (modifier) {
            is PaddingModifier -> acc.applyPadding(modifier)
            is SizeModifier -> when {
                modifier.width != null && modifier.height != null ->
                    acc.size(modifier.width.dp, modifier.height.dp)
                modifier.width != null -> acc.width(modifier.width.dp)
                else -> acc.height(modifier.height!!.dp)
            }
            is FillMaxWidthModifier -> acc.fillMaxWidth(modifier.fraction ?: 1f)
            is FillMaxHeightModifier -> acc.fillMaxHeight(modifier.fraction ?: 1f)
            is FillMaxSizeModifier -> acc.fillMaxSize(modifier.fraction ?: 1f)
            is BackgroundModifier -> acc.background(parseKomposerColor(modifier.color))
            is WeightModifier -> scope?.weight(acc, modifier.value, modifier.fill ?: true)
                ?: throw KomposerRenderException(
                    "weight modifier requires a Column (or Row) parent"
                )
        }
    }

private fun Modifier.applyPadding(m: PaddingModifier): Modifier = when {
    m.all != null -> padding(m.all.dp)
    m.horizontal != null || m.vertical != null ->
        padding(horizontal = (m.horizontal ?: 0f).dp, vertical = (m.vertical ?: 0f).dp)
    else -> padding(
        start = (m.start ?: 0f).dp, top = (m.top ?: 0f).dp,
        end = (m.end ?: 0f).dp, bottom = (m.bottom ?: 0f).dp,
    )
}
