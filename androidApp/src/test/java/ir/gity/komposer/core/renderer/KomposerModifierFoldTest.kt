package ir.gity.komposer.core.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.gity.komposer.core.KomposerRenderException
import ir.gity.komposer.core.model.modifier.BackgroundModifier
import ir.gity.komposer.core.model.modifier.PaddingModifier
import ir.gity.komposer.core.model.modifier.WeightModifier
import ir.gity.komposer.core.widget.parseKomposerColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

/**
 * `Modifier.Element` implementations define structural equality (required for recomposition
 * skipping), so folded chains compare with `==`.
 */
class KomposerModifierFoldTest {

    @Test
    fun foldAppliesModifiersInListOrder() {
        val color = parseKomposerColor("#FFD54F")
        val folded = listOf(
            BackgroundModifier(color = "#FFD54F"),
            PaddingModifier(all = 8f),
        ).toComposeModifier()

        // Equal to the hand-built chain in the same order.
        assertEquals(Modifier.background(color).padding(8.dp), folded)
    }

    @Test
    fun reversedFoldDiffersFromForward() {
        val forward = listOf(
            BackgroundModifier(color = "#FFD54F"),
            PaddingModifier(all = 8f),
        ).toComposeModifier()
        val reversed = listOf(
            PaddingModifier(all = 8f),
            BackgroundModifier(color = "#FFD54F"),
        ).toComposeModifier()

        // Order is semantics: background→padding ≠ padding→background.
        assertNotEquals(forward, reversed)
        val color = parseKomposerColor("#FFD54F")
        assertEquals(Modifier.padding(8.dp).background(color), reversed)
    }

    @Test
    fun paddingGroupsDispatchToMatchingOverloads() {
        assertEquals(
            Modifier.padding(8.dp),
            listOf(PaddingModifier(all = 8f)).toComposeModifier(),
        )
        assertEquals(
            Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            listOf(PaddingModifier(horizontal = 12f, vertical = 4f)).toComposeModifier(),
        )
        // Absent axis ⇒ 0.
        assertEquals(
            Modifier.padding(horizontal = 12.dp, vertical = 0.dp),
            listOf(PaddingModifier(horizontal = 12f)).toComposeModifier(),
        )
        assertEquals(
            Modifier.padding(start = 1.dp, top = 2.dp, end = 3.dp, bottom = 4.dp),
            listOf(PaddingModifier(start = 1f, top = 2f, end = 3f, bottom = 4f)).toComposeModifier(),
        )
        // Absent edge ⇒ 0.
        assertEquals(
            Modifier.padding(start = 1.dp, top = 0.dp, end = 3.dp, bottom = 0.dp),
            listOf(PaddingModifier(start = 1f, end = 3f)).toComposeModifier(),
        )
    }

    @Test
    fun weightWithNoScopeThrows() {
        assertFailsWith<KomposerRenderException> {
            listOf(WeightModifier(value = 1f)).toComposeModifier(scope = null)
        }
    }

    @Test
    fun weightPassesValueAndFillToScopeInListPosition() {
        val stub = RecordingScope()
        listOf(
            PaddingModifier(all = 8f),
            WeightModifier(value = 2f, fill = false),
        ).toComposeModifier(stub)

        assertEquals(2f, stub.value)
        assertEquals(false, stub.fill)
        // "In list position": weight received the accumulator with the preceding padding folded.
        assertEquals(Modifier.padding(8.dp), stub.receivedModifier)
    }

    @Test
    fun weightFillDefaultsToTrueWhenAbsent() {
        val stub = RecordingScope()
        listOf(WeightModifier(value = 3f)).toComposeModifier(stub)
        assertEquals(3f, stub.value)
        assertEquals(true, stub.fill)
    }

    private class RecordingScope : KomposerRenderScope {
        var receivedModifier: Modifier? = null
        var value: Float? = null
        var fill: Boolean? = null
        override fun weight(modifier: Modifier, value: Float, fill: Boolean): Modifier {
            this.receivedModifier = modifier
            this.value = value
            this.fill = fill
            return modifier
        }
    }
}
