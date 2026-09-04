package ir.gity.komposer.core.model.layout

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Container layout vocabulary: node fields on `column` / `row` / `box`, not modifiers — in Compose
// they are container parameters, not `Modifier` calls, and the wire mirrors that split.
//
// Tokens are per-axis vocabularies, designed once: horizontal-axis positions are start/center/end;
// vertical-axis positions are top/center/bottom; main-axis distribution adds
// spaceBetween/spaceAround/spaceEvenly. Each axis is its own enum class, deliberately — Compose's
// `Arrangement.Horizontal` / `Arrangement.Vertical` (and the `Alignment` pair) are distinct types,
// and one enum spanning both axes would push the axis mismatch to render time.
//
// The parameterized `Arrangement.spacedBy` is not a token here: it reaches the wire as the
// containers' optional `spacing` dp field (see `RowModel` / `ColumnModel`).
//
// @Serializable is required on these enums: entry-level @SerialName is honored only by the
// plugin-generated enum serializer (same pattern as the text enums).

// --- column: main axis vertical, cross axis horizontal ---

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

// --- row: main axis horizontal, cross axis vertical ---

@Serializable
enum class HorizontalArrangementValue {
    @SerialName("start") Start,
    @SerialName("center") Center,
    @SerialName("end") End,
    @SerialName("spaceBetween") SpaceBetween,
    @SerialName("spaceAround") SpaceAround,
    @SerialName("spaceEvenly") SpaceEvenly,
}

@Serializable
enum class VerticalAlignmentValue {
    @SerialName("top") Top,
    @SerialName("center") Center,
    @SerialName("bottom") Bottom,
}

// --- box: no main axis, so a two-dimensional position ---

@Serializable
enum class AlignmentValue {
    @SerialName("topStart") TopStart,
    @SerialName("topCenter") TopCenter,
    @SerialName("topEnd") TopEnd,
    @SerialName("centerStart") CenterStart,
    @SerialName("center") Center,
    @SerialName("centerEnd") CenterEnd,
    @SerialName("bottomStart") BottomStart,
    @SerialName("bottomCenter") BottomCenter,
    @SerialName("bottomEnd") BottomEnd,
}
