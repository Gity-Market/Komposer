# SPEC-0002 — Node Catalog v1

**Status:** Implemented (2026-07-12)
**Depends on:** SPEC-0001 (wire conventions)
**Implemented by:** SPEC-0003 (models), SPEC-0004 (mapping to Compose)

## Scope

The exact field set, validation rules, and Compose mapping for the three v1
nodes: **text**, **column**, **spacer**. Text is deliberately rich (it's our
pilot for "map a real composable's surface onto the wire"); column and spacer
stay minimal until the modifier problem (roadmap Phase 3) is tackled.

## Non-goals

- `Modifier` in any form — no padding, size, background, weight, click. That is
  the Phase 3 problem and nothing here may pre-empt it.
- `fontFamily` — needs a font-registry design (server sends a token, client
  resolves to a bundled/downloadable font). Deferred; tracked in Open questions.
- Column arrangement/alignment — arrives together with the modifier design so
  layout vocabulary is designed once, not twice.

---

## 1. `text`

Maps to `androidx.compose.material3.Text`.

### Wire fields

| Field | Wire type | Required | Default when absent | Compose mapping |
| --- | --- | --- | --- | --- |
| `text` | string | **yes** | — | `text` (empty string is legal) |
| `color` | color string | no | `Color.Unspecified` (theme decides) | `color` |
| `fontSize` | number (sp) | no | `TextUnit.Unspecified` | `fontSize` |
| `fontWeight` | integer 1..1000 | no | `null` | `FontWeight(value)` |
| `fontStyle` | `"normal"` \| `"italic"` | no | `null` | `FontStyle.Normal` / `FontStyle.Italic` |
| `letterSpacing` | number (sp) | no | `TextUnit.Unspecified` | `letterSpacing` |
| `textDecoration` | `"none"` \| `"underline"` \| `"lineThrough"` | no | `null` | `TextDecoration.*` |
| `textAlign` | `"start"` \| `"end"` \| `"center"` \| `"justify"` \| `"left"` \| `"right"` | no | `null` | `TextAlign.*` |
| `lineHeight` | number (sp) | no | `TextUnit.Unspecified` | `lineHeight` |
| `overflow` | `"clip"` \| `"ellipsis"` \| `"visible"` | no | `TextOverflow.Clip` | `overflow` |
| `softWrap` | boolean | no | `true` | `softWrap` |
| `maxLines` | integer ≥ 1 | no | unbounded (`Int.MAX_VALUE`) | `maxLines` |
| `minLines` | integer ≥ 1 | no | `1` | `minLines` |

### Kotlin model

```kotlin
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
) : KomposerModel { init { /* §4 validation */ } }
```

Enums are `@Serializable` Kotlin enum classes in `commonMain`, one per closed
set, with `@SerialName` carrying the wire token:

```kotlin
// @Serializable is required here: entry-level @SerialName is honored only by
// the plugin-generated enum serializer.
@Serializable
enum class FontStyleValue {
    @SerialName("normal") Normal,
    @SerialName("italic") Italic,
}
// TextDecorationValue, TextAlignValue, TextOverflowValue: same pattern,
// tokens exactly as in the table above.
```

Rationale: enum classes give exhaustive `when` in the mapping layer and make an
illegal token a *parse-time* failure, keeping raw strings out of the engine.

### Fields intentionally excluded from the model

These exist on `TextWidget` / Compose `Text` but must **not** appear on the
wire or in `TextModel`:

- `modifier`, `style` — Phase 3.
- `fontFamily` — needs the font-registry design.
- `onTextLayout` — a client-side callback; behavior is never data.

### Notes

- `text` is **required and non-null** — this is a breaking change from today's
  `TextModel(val text: String? = null)`. An empty string is legal;
  `NonEmptyTextSpecification` remains a pattern demo, not an enforced rule.
- Explicitly setting an optional field to its client-side default (e.g.
  `"overflow": "clip"`) is legal and renders correctly, but is *non-canonical*:
  the widget layer normalizes it back to absent on `toModel()` (SPEC-0004 §4).

## 2. `column`

Maps to `androidx.compose.foundation.layout.Column`.

### Wire fields

| Field | Wire type | Required | Default when absent | Compose mapping |
| --- | --- | --- | --- | --- |
| `children` | array of nodes | no | `[]` | column content, in order |

### Kotlin model

```kotlin
@Serializable
@SerialName("column")
data class ColumnModel(
    val children: List<KomposerModel> = emptyList(),
) : KomposerModel
```

Note: plain `List<KomposerModel>` — the current `@Contextual` annotation is
wrong and is removed (SPEC-0003 §3). Interface-typed properties serialize
polymorphically via the registered module.

### Render behavior (v1, unchanged from today)

`Column(modifier = Modifier.fillMaxWidth())`, children rendered in array
order. The `fillMaxWidth` is an interim hardcode; it dissolves into the
modifier system in Phase 3.

## 3. `spacer`

Maps to `androidx.compose.foundation.layout.Spacer`.

### Wire fields

| Field | Wire type | Required | Default when absent | Compose mapping |
| --- | --- | --- | --- | --- |
| `height` | number (dp) ≥ 0 | **yes** | — | `Modifier.height(height.dp)` |

### Kotlin model

```kotlin
@Serializable
@SerialName("spacer")
data class SpacerModel(
    val height: Float,
) : KomposerModel { init { require(height >= 0f) { "spacer height must be >= 0" } } }
```

### Notes — this fixes three existing bugs at once

- The field is renamed **`px` → `height` and reinterpreted as dp**. Pixels on
  the wire were wrong (server can't know density), and dp removes
  `SpacerWidgetFactory`'s `Density` dependency entirely (SPEC-0004 §3).
- `SpacerModel.toWidget()` currently ignores `px` and hardcodes `16.dp`; and
  `SpacerWidget.toModel()` hardcodes `26f`. Both silent-lie paths are removed
  by SPEC-0003/0004 (single construction path, faithful `toModel`).
- Render keeps today's `fillMaxWidth().height(h.dp)` — a vertical spacer,
  matching its use inside columns. A width/size spacer waits for Phase 3.

## 4. Validation

Rules live in model `init` blocks **using `require`** (pure Kotlin, so they run
in `commonMain`; kotlinx.serialization invokes `init` blocks during
deserialization exactly like a regular constructor, so a bad payload fails at
parse time). `require` throws `IllegalArgumentException`, which SPEC-0003 §5
wraps into `KomposerParseException`. Never use `check` here — its
`IllegalStateException` would escape that wrapper.

| Node | Rule |
| --- | --- |
| `text` | `color` matches `^#([0-9a-fA-F]{6}\|[0-9a-fA-F]{8})$` when present |
| `text` | `fontWeight in 1..1000` when present |
| `text` | `maxLines >= 1`, `minLines >= 1`, `maxLines >= minLines` (when both present) |
| `text` | `fontSize`, `lineHeight` are finite and `> 0` when present |
| `text` | `letterSpacing` is finite when present (zero and *negative* tracking are legal typography) |
| `spacer` | `height >= 0` and finite |

Enum tokens are validated by serialization itself (unknown token ⇒ failure).

## Acceptance criteria

- [ ] A fully-populated `text` node (every field set) round-trips `JSON ⇄ model`
      (SPEC-0003) and renders with every attribute visibly applied. (The
      widget-level round-trip is SPEC-0004 §4's and holds for canonical values.)
- [ ] A minimal `{"type":"text","text":"hi"}` renders identically to
      `Text("hi")` with all defaults.
- [ ] Each validation rule in §4 has a test proving the bad payload fails to parse.
- [ ] `spacer` height on screen matches the same dp value hardcoded in Compose
      (manual check on one device is fine for v1).

## Open questions (deliberately deferred)

- `fontFamily` as a server token + client font registry.
- Column `verticalArrangement` / `horizontalAlignment` — designed with modifiers.
- Should color accept theme *references* (`"primary"`) instead of raw hex? Almost
  certainly yes eventually; needs the theming story first.
