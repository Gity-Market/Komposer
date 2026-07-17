package ir.gity.komposer.core.model.layout

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Column layout vocabulary (SPEC-0005 §3): node fields on `column`, not modifiers — in
// Compose they are `Column` parameters, not `Modifier` calls, and the wire mirrors that split.
//
// Tokens are per-axis vocabularies ("designed once" for Phase 4's Row): horizontal-axis
// positions are start/center/end; vertical-axis positions are top/center/bottom; main-axis
// distribution adds spaceBetween/spaceAround/spaceEvenly. Row's enums land beside these.
//
// @Serializable is required on these enums: entry-level @SerialName is honored only by the
// plugin-generated enum serializer (same pattern as SPEC-0002's text enums).

@Serializable
enum class VerticalArrangementValue {
    @SerialName("top") Top,
    @SerialName("center") Center,
    @SerialName("bottom") Bottom,
    @SerialName("spaceBetween") SpaceBetween,
    @SerialName("spaceAround") SpaceAround,
    @SerialName("spaceEvenly") SpaceEvenly,
}

@Serializable
enum class HorizontalAlignmentValue {
    @SerialName("start") Start,
    @SerialName("center") Center,
    @SerialName("end") End,
}
