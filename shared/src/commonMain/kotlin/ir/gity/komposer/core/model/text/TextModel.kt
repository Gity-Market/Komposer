package ir.gity.komposer.core.model.text

import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.KomposerModelVisitor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Maps to `androidx.compose.material3.Text` (SPEC-0002 §1). All optional fields are
 * nullable and default to `null` ("unspecified"); the mapping layer (factory) applies
 * Compose defaults for absent values, not this model.
 */
@Serializable
@SerialName("text")
data class TextModel(
    val text: String,
    val color: String? = null,
    val fontSize: Float? = null,
    val fontWeight: Int? = null,
    val fontStyle: FontStyleValue? = null,
    val letterSpacing: Float? = null,
    val textDecoration: TextDecorationValue? = null,
    val textAlign: TextAlignValue? = null,
    val lineHeight: Float? = null,
    val overflow: TextOverflowValue? = null,
    val softWrap: Boolean? = null,
    val maxLines: Int? = null,
    val minLines: Int? = null,
) : KomposerModel {

    init {
        color?.let {
            require(COLOR_REGEX.matches(it)) {
                "color must match #RRGGBB or #AARRGGBB, was \"$it\""
            }
        }
        fontWeight?.let {
            require(it in 1..1000) { "fontWeight must be in 1..1000, was $it" }
        }
        fontSize?.let {
            require(it.isFinite() && it > 0f) { "fontSize must be finite and > 0, was $it" }
        }
        lineHeight?.let {
            require(it.isFinite() && it > 0f) { "lineHeight must be finite and > 0, was $it" }
        }
        letterSpacing?.let {
            require(it.isFinite()) { "letterSpacing must be finite, was $it" }
        }
        maxLines?.let { require(it >= 1) { "maxLines must be >= 1, was $it" } }
        minLines?.let { require(it >= 1) { "minLines must be >= 1, was $it" } }
        if (maxLines != null && minLines != null) {
            require(maxLines >= minLines) {
                "maxLines ($maxLines) must be >= minLines ($minLines)"
            }
        }
    }

    override fun accept(visitor: KomposerModelVisitor) = visitor.visit(this)

    companion object {
        val COLOR_REGEX = Regex("^#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")
    }
}

// @Serializable is required on these enums: entry-level @SerialName is honored only by
// the plugin-generated enum serializer. Tokens are the exact wire strings (SPEC-0002).

@Serializable
enum class FontStyleValue {
    @SerialName("normal") Normal,
    @SerialName("italic") Italic,
}

@Serializable
enum class TextDecorationValue {
    @SerialName("none") None,
    @SerialName("underline") Underline,
    @SerialName("lineThrough") LineThrough,
}

@Serializable
enum class TextAlignValue {
    @SerialName("start") Start,
    @SerialName("end") End,
    @SerialName("center") Center,
    @SerialName("justify") Justify,
    @SerialName("left") Left,
    @SerialName("right") Right,
}

@Serializable
enum class TextOverflowValue {
    @SerialName("clip") Clip,
    @SerialName("ellipsis") Ellipsis,
    @SerialName("visible") Visible,
}
