package ir.gity.komposer.core.model.modifier

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Maps to the three `Modifier.padding` overloads (SPEC-0005 §2.1). Exactly one **group** of
 * fields may be used per instance, mirroring Compose's overload set — Compose has no
 * "all plus start" overload, so the wire doesn't either.
 *
 * - Group A: `all`
 * - Group B: `horizontal` / `vertical` (absent axis ⇒ 0)
 * - Group C: `start` / `top` / `end` / `bottom` (absent edge ⇒ 0)
 */
@Serializable
@SerialName("padding")
data class PaddingModifier(
    val all: Float? = null,
    val horizontal: Float? = null,
    val vertical: Float? = null,
    val start: Float? = null,
    val top: Float? = null,
    val end: Float? = null,
    val bottom: Float? = null,
) : KomposerModifier {
    init {
        val groupsUsed = listOf(
            listOf(all),
            listOf(horizontal, vertical),
            listOf(start, top, end, bottom),
        ).count { group -> group.any { it != null } }
        require(groupsUsed >= 1) { "padding requires at least one field" }
        require(groupsUsed == 1) {
            "padding fields must come from one group: all | horizontal/vertical | start/top/end/bottom"
        }
        listOfNotNull(all, horizontal, vertical, start, top, end, bottom).forEach {
            require(it.isFinite() && it >= 0f) { "padding values must be finite and >= 0, was $it" }
        }
    }
}
