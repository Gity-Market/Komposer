package ir.gity.komposer.core.model.modifier

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `fillMaxWidth` / `fillMaxHeight` / `fillMaxSize`: three wire types, one
 * per Compose function. `fraction` defaults to `1` when absent; the `0 ≤ f ≤ 1` range is our
 * wire rule (Compose doesn't validate — we'd rather fail a nonsensical payload loudly).
 */

@Serializable
@SerialName("fillMaxWidth")
data class FillMaxWidthModifier(val fraction: Float? = null) : KomposerModifier {
    init { fraction?.let { require(it.isFinite() && it in 0f..1f) { "fraction must be in 0..1, was $it" } } }
}

@Serializable
@SerialName("fillMaxHeight")
data class FillMaxHeightModifier(val fraction: Float? = null) : KomposerModifier {
    init { fraction?.let { require(it.isFinite() && it in 0f..1f) { "fraction must be in 0..1, was $it" } } }
}

@Serializable
@SerialName("fillMaxSize")
data class FillMaxSizeModifier(val fraction: Float? = null) : KomposerModifier {
    init { fraction?.let { require(it.isFinite() && it in 0f..1f) { "fraction must be in 0..1, was $it" } } }
}
