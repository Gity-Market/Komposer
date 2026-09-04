package ir.gity.komposer.core.widget

import androidx.compose.ui.layout.ContentScale
import ir.gity.komposer.core.model.ContentScaleValue
import ir.gity.komposer.core.model.ImageModel
import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.modifier.KomposerModifier

// Compose-typed storage with defaults matching Compose (`ContentScale.Fit`, no description), the
// TextWidget strategy. The URL stays a string: the widget is Compose-aware, not network-aware —
// resolving it is the renderer's (Coil's) job.
data class ImageWidget(
    val url: String,
    val contentDescription: String? = null,
    val contentScale: ContentScale = ContentScale.Fit,
    override val modifiers: List<KomposerModifier> = emptyList(),
) : KomposerWidget {

    // Faithful, normalized to canonical form: the Compose default collapses to absent; modifiers
    // and strings copy through verbatim.
    override fun toModel(): KomposerModel = ImageModel(
        url = url,
        contentDescription = contentDescription,
        contentScale = contentScale.toValue(),
        modifiers = modifiers,
    )

    // Default (Fit) → null; the six other named scales → their token; an arbitrary hand-built
    // ContentScale → null, collapsing to absent (same policy as TextAlign's `else -> null`).
    private fun ContentScale.toValue(): ContentScaleValue? = when (this) {
        ContentScale.Fit -> null
        ContentScale.Crop -> ContentScaleValue.Crop
        ContentScale.FillBounds -> ContentScaleValue.FillBounds
        ContentScale.FillWidth -> ContentScaleValue.FillWidth
        ContentScale.FillHeight -> ContentScaleValue.FillHeight
        ContentScale.Inside -> ContentScaleValue.Inside
        ContentScale.None -> ContentScaleValue.None
        else -> null
    }
}

/** Maps every wire field, applying Compose defaults for absent ones. */
fun ImageModel.toWidget(): ImageWidget = ImageWidget(
    url = url,
    contentDescription = contentDescription,
    contentScale = contentScale.toContentScale(),
    // The mapping's whole modifier job is a faithful copy.
    modifiers = modifiers,
)

// Absent (null) → Compose default. Exhaustive `when` over the closed enum.
private fun ContentScaleValue?.toContentScale(): ContentScale = when (this) {
    null -> ContentScale.Fit
    ContentScaleValue.Fit -> ContentScale.Fit
    ContentScaleValue.Crop -> ContentScale.Crop
    ContentScaleValue.FillBounds -> ContentScale.FillBounds
    ContentScaleValue.FillWidth -> ContentScale.FillWidth
    ContentScaleValue.FillHeight -> ContentScale.FillHeight
    ContentScaleValue.Inside -> ContentScale.Inside
    ContentScaleValue.None -> ContentScale.None
}
