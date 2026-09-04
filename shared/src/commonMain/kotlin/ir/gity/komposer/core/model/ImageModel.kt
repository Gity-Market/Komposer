package ir.gity.komposer.core.model

import ir.gity.komposer.core.model.modifier.KomposerModifier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An image loaded from a URL — rendered by `coil3.compose.AsyncImage` on Android.
 *
 * `url` is the only source v1 accepts, and the field is named for exactly that: a future bundled
 * `resource` alternative can join as a mutually exclusive sibling (the `padding` groups pattern)
 * instead of overloading one field. `contentDescription` is the accessibility text; absent means
 * decorative (`null` in Compose). `contentScale` is a **node field** — an `Image` parameter, not a
 * `Modifier` call. Sizing comes from modifiers (`size`, `fillMaxWidth`, …); with none, the image
 * lays out at its intrinsic size once loaded, and at zero size until then.
 *
 * `modifiers` is the last constructor parameter so positional construction sites keep compiling.
 */
@Serializable
@SerialName("image")
data class ImageModel(
    val url: String,
    val contentDescription: String? = null,
    val contentScale: ContentScaleValue? = null,
    override val modifiers: List<KomposerModifier> = emptyList(),
) : KomposerModel {
    init {
        require(url.isNotBlank()) { "image url must not be blank" }
    }
}

// @Serializable is required: entry-level @SerialName is honored only by the plugin-generated
// enum serializer. Tokens are the exact wire strings, mirroring Compose's `ContentScale` names.
@Serializable
enum class ContentScaleValue {
    @SerialName("fit") Fit,
    @SerialName("crop") Crop,
    @SerialName("fillBounds") FillBounds,
    @SerialName("fillWidth") FillWidth,
    @SerialName("fillHeight") FillHeight,
    @SerialName("inside") Inside,
    @SerialName("none") None,
}
