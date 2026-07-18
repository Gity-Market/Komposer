# SPEC-0006 — Row Node & Spacing

**Status:** Proposed (2026-07-18)
**Depends on:** SPEC-0001 (wire conventions), SPEC-0002 (node catalog), SPEC-0003 (shared models & serializer), SPEC-0004 (rendering pipeline), SPEC-0005 (modifiers, layout vocabulary, render scope)
**Delivers:** the first Phase 4 node — `column`'s horizontal counterpart — plus the deferred `Arrangement.spacedBy` decision

## Scope

The `row` node: wire fields, shared model, widget, factory, renderer, and the
`RowRenderScope` that makes `weight` work horizontally. This spec also settles
the question SPEC-0005 deferred "until Row lands": how `Arrangement.spacedBy`
reaches the wire. The answer — an optional `spacing` dp field — lands on
**both** `row` and `column` here, so the gap vocabulary is designed once,
exactly as the arrangement/alignment vocabulary was in SPEC-0005 §3.

Everything vocabulary-shaped was pre-decided by SPEC-0005 §3's "designed once"
contract; this spec is deliberately mostly transcription of that contract into
node fields.

## Non-goals

- **`Box`** — the third container has no main axis, so it needs an
  *two-dimensional* alignment vocabulary (`topStart` … `bottomEnd`) plus
  per-child alignment. Its own catalog entry, not a rider here.
- **A horizontal `spacer`** — `spacer.height` stays required and vertical
  (SPEC-0002 §3). Inside a row, gaps come from `spacing` (this spec) or a
  `size` modifier on an invisible node; relaxing `spacer` to
  width-and/or-height is tracked in Open questions.
- **Negative `spacing`** — Compose accepts `spacedBy(-8.dp)` (children
  overlap; avatar stacks), but v1 keeps the same conservative posture as
  padding. Open questions.
- Combining `spacing` with a *positional* arrangement token (Compose's
  `spacedBy(space, alignment)` overloads). v1 makes `spacing` and the
  arrangement field mutually exclusive; the combination is an Open question.
- Lazy variants (`LazyRow`), scrolling, `weight` changes — `WeightModifier`
  already exists (SPEC-0005 §2.5) and gains Row support purely through the
  scope mechanism (§5).

---

## 1. `row`

Maps to `androidx.compose.foundation.layout.Row`.

### Wire fields

| Field | Wire type | Required | Default when absent | Compose mapping |
| --- | --- | --- | --- | --- |
| `children` | array of nodes | no | `[]` | row content, in order |
| `horizontalArrangement` | enum token (§2) | no | `Arrangement.Start` | `horizontalArrangement` |
| `verticalAlignment` | enum token (§2) | no | `Alignment.Top` | `verticalAlignment` |
| `spacing` | number (dp) ≥ 0 | no | no gap | `horizontalArrangement = Arrangement.spacedBy(spacing.dp)` (§3) |

Plus `modifiers`, which every node carries via `KomposerModel` (SPEC-0005 §1) —
nothing row-specific there.

`spacing` and `horizontalArrangement` are **mutually exclusive** (§3); setting
both fails parsing.

### Kotlin model

```kotlin
// shared/src/commonMain/kotlin/ir/gity/komposer/core/model/row/RowModel.kt
@Serializable
@SerialName("row")
data class RowModel(
    val children: List<KomposerModel> = emptyList(),
    val horizontalArrangement: HorizontalArrangementValue? = null,
    val verticalAlignment: VerticalAlignmentValue? = null,
    val spacing: Float? = null,
    override val modifiers: List<KomposerModifier> = emptyList(),
) : KomposerModel {
    init {
        spacing?.let {
            require(it.isFinite() && it >= 0f) { "spacing must be finite and >= 0, was $it" }
            require(horizontalArrangement == null) {
                "spacing and horizontalArrangement are mutually exclusive (spacing IS an arrangement)"
            }
        }
    }
    override fun accept(visitor: KomposerModelVisitor) = visitor.visit(this)
}
```

`modifiers` stays the last constructor parameter, per the SPEC-0005 §4
convention.

## 2. The per-axis enums

New enum classes in `core/model/layout/LayoutValues.kt`, beside the Column
pair, following the SPEC-0002 enum pattern (`@Serializable` enum class,
entry-level `@SerialName` wire tokens). The tokens are exactly the ones
SPEC-0005 §3 promised: horizontal-axis positions are `start`/`center`/`end`,
vertical-axis positions are `top`/`center`/`bottom`, main-axis distribution
adds the three `space*` tokens.

```kotlin
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
```

| `horizontalArrangement` token | Compose value |
| --- | --- |
| `start` | `Arrangement.Start` |
| `center` | `Arrangement.Center` |
| `end` | `Arrangement.End` |
| `spaceBetween` | `Arrangement.SpaceBetween` |
| `spaceAround` | `Arrangement.SpaceAround` |
| `spaceEvenly` | `Arrangement.SpaceEvenly` |

| `verticalAlignment` token | Compose value |
| --- | --- |
| `top` | `Alignment.Top` |
| `center` | `Alignment.CenterVertically` |
| `bottom` | `Alignment.Bottom` |

These are **new enum classes**, not reuses: SPEC-0005 §3 explicitly forbids
pretending one enum covers both axes (Compose's `Arrangement.Horizontal` and
`Arrangement.Vertical` are distinct types; sharing an enum would push the axis
mismatch to render time).

## 3. `spacing` — the `Arrangement.spacedBy` decision

SPEC-0005 deferred `spacedBy` because a *parameterized* arrangement cannot be
a closed-enum token, and named two candidate shapes: a sibling dp field, or
promoting arrangement from token to object. **Decision: the sibling field.**

```json
{ "type": "row", "spacing": 8, "children": [ … ] }
```

- **Why not an object?** Promoting `horizontalArrangement` to
  `{"kind": "spacedBy", "space": 8}` makes every *existing* token payload a
  breaking change or forces a string-or-object union — kotlinx.serialization
  handles unions poorly, and SPEC-0001 §3's "enum = string token from a closed
  set" convention would grow its first exception. A sibling number field costs
  one row in the field table and keeps every convention intact.
- **Why mutually exclusive with the arrangement field?** In Compose,
  `spacedBy` *is* an arrangement — `Row` takes exactly one
  `horizontalArrangement` argument, so `"spacing": 8` with
  `"horizontalArrangement": "spaceBetween"` is a contradiction (two
  arrangements, one slot). The wire mirrors Compose's own signature instead of
  inventing a precedence rule. Compose's `spacedBy(space, alignment)` overloads
  *can* combine a gap with a positional pack — that combination is deferred
  (Open questions), so v1 exclusion covers the whole field, not just the
  `space*` tokens.
- **Validation:** `spacing` must be finite and ≥ 0 (`require` in `init`, §1).
  Zero is legal (renders identically to absent — non-canonical in the
  SPEC-0004 §4 sense, but round-trips exactly; see §6).
- **`column` gains the same field**, same rules, mutually exclusive with
  `verticalArrangement` — the gap vocabulary exists once:

```kotlin
@Serializable
@SerialName("column")
data class ColumnModel(
    val children: List<KomposerModel> = emptyList(),
    val verticalArrangement: VerticalArrangementValue? = null,
    val horizontalAlignment: HorizontalAlignmentValue? = null,
    val spacing: Float? = null,                    // NEW — same init rules as RowModel
    override val modifiers: List<KomposerModifier> = emptyList(),
) : KomposerModel
```

This extends SPEC-0002 §2 / SPEC-0005 §3 (their field tables gain the row
above); adding an optional field to an existing node is the forward-compat
door SPEC-0001 §5 deliberately keeps open — old clients ignore it. (They
render *without* the gap, which is degraded but not lying; a server targeting
old clients simply doesn't send `spacing`.)

## 4. Registration — the five touch-points

Per the standing checklist (SPEC-0004 migration notes; CLAUDE.md "Adding a new
widget type"), `row` touches:

1. `KomposerSchema` — `subclass(RowModel::class)`.
2. Factory registration at the composition root (`v1Registry()` in
   `MainActivity.kt`) — `register<RowModel>(RowWidgetFactory())`.
3. `KomposerRenderer` — `is RowWidget -> RenderRow(widget, scope)`.
4. `KomposerWidgetVisitor` — a `Visit(rowWidget: RowWidget)` overload, plus
   `GraphBuilder`'s dispatch `when`.
5. `KomposerModelVisitor` — a `visit(rowModel: RowModel)` overload.

If SPEC-0007 (single-point registration) is accepted and implemented first,
points 1–4 collapse into one `KomposerNodeRegistration` and point 5 becomes
optional (generic fallback); `row` would then ship as that design's first
proof. This spec is written against the *current* five-point world and does
not depend on SPEC-0007.

## 5. Mapping to Compose (`androidApp`)

New files under `androidApp/.../core/widget/row/`: `RowWidget.kt`,
`RenderRow.kt`; plus `factory/RowWidgetFactory.kt` and
`renderer/RowRenderScope` (beside `ColumnRenderScope`).

### 5.1 `RowWidget` — the `ColumnWidget` pattern, plus `spacing`

Compose-typed storage with defaults matching Compose (SPEC-0005 §5.6), and one
new wrinkle: `spacing` is stored as its own `Dp?`, **not** folded into the
arrangement:

```kotlin
class RowWidget(
    children: MutableList<KomposerWidget> = mutableListOf(),
    val horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    val verticalAlignment: Alignment.Vertical = Alignment.Top,
    val spacing: Dp? = null,
    override val modifiers: List<KomposerModifier> = emptyList(),
) : KomposerWidget, KomposerCompositeWidget {
    init {
        require(spacing == null || horizontalArrangement == Arrangement.Start) {
            "spacing and a non-default horizontalArrangement are mutually exclusive"
        }
    }
    // addChild/removeChild/getChildren/Accept: identical to ColumnWidget.
}
```

Why not store `Arrangement.spacedBy(spacing)` directly in
`horizontalArrangement`? Because `toModel()` could not reliably get the dp
back out — `spacedBy` returns an internal `Arrangement` implementation, and
recovering its parameter is exactly the "folded black box" problem SPEC-0005
§5.1 rejected for modifiers. A separate `Dp?` field round-trips exactly and
the render site composes the two (§5.3). The widget-level `require` mirrors
the model rule so a hand-built widget can't reach `toModel()` with a
contradiction the model's `init` would reject.

### 5.2 `RowWidgetFactory` — the `ColumnWidgetFactory` pattern

Recurses through `root`, copies `modifiers` verbatim, maps the enums with
exhaustive `when`s (absent ⇒ Compose default), and maps
`spacing?.dp`. `ColumnWidgetFactory` gains the same one-liner for column's new
`spacing`.

### 5.3 `RenderRow` and `RowRenderScope`

```kotlin
class RowRenderScope(private val scope: RowScope) : KomposerRenderScope {
    override fun weight(modifier: Modifier, value: Float, fill: Boolean): Modifier =
        with(scope) { modifier.weight(value, fill) }
}

@Composable
fun RenderRow(widget: RowWidget, scope: KomposerRenderScope? = null) {
    Row(
        modifier = widget.modifiers.toComposeModifier(scope),
        horizontalArrangement = widget.spacing?.let { Arrangement.spacedBy(it) }
            ?: widget.horizontalArrangement,
        verticalAlignment = widget.verticalAlignment,
    ) {
        val childScope = RowRenderScope(this)
        widget.getChildren().forEach { child -> KomposerRenderer(child, childScope) }
    }
}
```

This is the "nothing else changes" SPEC-0005 §5.3 promised: `WeightModifier`,
the fold, and `KomposerRenderScope` are untouched — a `weight` inside a row
now finds a scope and works; at the root or under a scope-less parent it still
throws `KomposerRenderException`. The fold's error message ("weight modifier
requires a Column (or Row) parent") was written for this day and stays as-is.

`RenderColumn` gains the mirrored `spacing` line
(`verticalArrangement = widget.spacing?.let { Arrangement.spacedBy(it) } ?: widget.verticalArrangement`).

Like `column` after SPEC-0005 §5.5, a bare `row` wraps its content — full
width is `{"type": "fillMaxWidth"}`, stated on the wire, never hardcoded.

## 6. Round-trip & canonical form

Extends SPEC-0004 §4 / SPEC-0005 §6; the three regimes already defined cover
everything new:

- **`modifiers`:** exact for every list (verbatim storage — unchanged).
- **`horizontalArrangement` / `verticalAlignment`:** normalized like column's
  layout fields — Compose defaults (`Arrangement.Start`, `Alignment.Top`)
  come back as `null`; other representable values come back as their token;
  unrepresentable hand-built values collapse to `null`:

| Widget value | Model field becomes |
| --- | --- |
| `Arrangement.Start` | `null` |
| `Arrangement.Center` / `End` / `SpaceBetween` / `SpaceAround` / `SpaceEvenly` | the matching token |
| any other `Arrangement.Horizontal` | `null` |
| `Alignment.Top` | `null` |
| `Alignment.CenterVertically` / `Bottom` | the matching token |
| any other `Alignment.Vertical` | `null` |

- **`spacing`:** exact for every value, including `0` — stored as `Dp?`, the
  storage is not lossy, so like modifiers there is nothing to normalize
  (`"spacing": 0` is non-canonical on the wire — it renders identically to
  absent — but round-trips to itself; canonical form simply means absent-or-
  positive here).

**Invariant (tested):** canonical `row`/`column` models (layout fields absent
or non-default; `spacing` absent or any legal value; `modifiers`
unconstrained) satisfy `registry.build().create(m).toModel() == m`;
normalization stays idempotent; `JSON → model → widget → model → JSON` stays
lossless for canonical payloads.

## 7. Validation

All `require` in `init`, parse-time, wrapped into `KomposerParseException`
(SPEC-0003 §5 machinery). Never `check`.

| Model | Rule |
| --- | --- |
| `row` | `spacing` finite and ≥ 0 when present |
| `row` | `spacing` and `horizontalArrangement` not both present |
| `column` | `spacing` finite and ≥ 0 when present *(new)* |
| `column` | `spacing` and `verticalArrangement` not both present *(new)* |

Enum tokens are validated by serialization itself; `weight` placement stays
render-time (SPEC-0005 §2.5) — SPEC-0008 proposes the parse-time front-run.

## 8. Strictness additions (extends SPEC-0001 §5, SPEC-0005 §8)

| Situation | Behavior |
| --- | --- |
| Unknown `horizontalArrangement`/`verticalAlignment` token | **Fail** (serialization) |
| `spacing` + `horizontalArrangement` on `row` (or `spacing` + `verticalArrangement` on `column`) | **Fail** (`KomposerParseException`, §7) |
| `spacing` on a payload sent to a pre-0006 client | **Ignored** (unknown-field rule) — renders without the gap |
| `weight` on a direct child of `row` | Works (scope provided, §5.3) |

## 9. Reference payload

Exercises both new enums, `spacing` on both containers, `weight` inside a row,
and a nested column — the Phase 4 acceptance surface for this node:

```json
{
  "version": 1,
  "root": {
    "type": "column",
    "modifiers": [ { "type": "fillMaxSize" }, { "type": "padding", "all": 16 } ],
    "spacing": 12,
    "children": [
      {
        "type": "row",
        "modifiers": [ { "type": "fillMaxWidth" } ],
        "verticalAlignment": "center",
        "spacing": 8,
        "children": [
          { "type": "text", "text": "8dp gaps", "fontWeight": 700 },
          { "type": "text", "text": "between" },
          { "type": "text", "text": "us" }
        ]
      },
      {
        "type": "row",
        "modifiers": [ { "type": "fillMaxWidth" } ],
        "horizontalArrangement": "spaceBetween",
        "children": [
          { "type": "text", "text": "far left" },
          { "type": "text", "text": "far right" }
        ]
      },
      {
        "type": "row",
        "modifiers": [ { "type": "fillMaxWidth" }, { "type": "background", "color": "#EDE7F6" } ],
        "verticalAlignment": "bottom",
        "children": [
          {
            "type": "text",
            "text": "weighted: I take the leftover width",
            "modifiers": [ { "type": "weight", "value": 1 } ]
          },
          { "type": "text", "text": "fixed", "fontStyle": "italic" },
          {
            "type": "column",
            "modifiers": [ { "type": "padding", "all": 4 } ],
            "children": [
              { "type": "text", "text": "nested", "fontSize": 12 },
              { "type": "text", "text": "column", "fontSize": 12 }
            ]
          }
        ]
      }
    ]
  }
}
```

## 10. Tests

`commonTest` (all KMP targets):

| Test | Asserts |
| --- | --- |
| Round-trip, minimal | `{"type":"row"}` ⇄ `RowModel()` |
| Round-trip, full | every field set (each arrangement/alignment token; spacing via a second variant, given exclusivity) |
| `spacing` round-trip | `spacing` on `row` and `column`, including `0`, survives `parseNode(encodeNode(m)) == m` |
| Nested tree | row-in-column-in-row preserves order and depth |
| §7 rules | each bad payload → `KomposerParseException` (non-finite spacing, negative spacing, spacing+arrangement on both node types) |
| Unknown tokens | `"horizontalArrangement": "diagonal"` → fail |
| §9 payload | parses; tree asserted field-by-field; re-encode → re-parse → equal |
| Encoding minimality | default `RowModel` encodes as `{"type":"row"}` |

`androidApp` unit tests:

| Test | Asserts |
| --- | --- |
| Widget round-trip | canonical `RowModel`s satisfy `create(m).toModel() == m`; explicit `"horizontalArrangement": "start"` / `"verticalAlignment": "top"` normalize to absent; idempotence holds; `spacing` survives exactly on both node types |
| Weight scope | a stub `RowRenderScope`-style scope receives `(value, fill)`; fold with `scope = null` still throws (regression) |
| Factory recursion | a custom factory registered for `TextModel` applies to texts nested inside a `row` (the SPEC-0004 §2 bypass bug never returns) |

## Migration notes

Breaking or visible; shared first, then `androidApp`:

1. **`ColumnModel` gains `spacing: Float? = null`** before `modifiers`
   (`modifiers` stays last, SPEC-0005 §4). Call sites passing `modifiers`
   *positionally* after `horizontalAlignment` break — switch to named
   arguments; sites using named arguments (all current ones) are unaffected.
2. `ColumnWidget` gains `spacing: Dp? = null` + the §5.1 `require`;
   `ColumnWidgetFactory` maps it; `RenderColumn` composes it (§5.3);
   `ColumnWidget.toModel()` returns it verbatim.
3. New registrations per §4 (five touch-points), including the two visitor
   overloads.
4. SPEC-0002 §2's `column` field table and SPEC-0005 §3's layout vocabulary
   are extended by this spec's `spacing` rows (recorded here; those documents
   are not edited — supersession flows forward, as with SPEC-0005 §5.5 over
   SPEC-0002's render notes).
5. `specs/README.md` index gains this spec's row; `ROADMAP.md` Phase 4 links
   it.
6. `MainActivity`'s demo payload may adopt §9 (or keep SPEC-0005 §9's —
   either renders; §9 above is the acceptance surface for *this* spec).

## Acceptance criteria

- [ ] The §9 reference payload parses and renders on a device/emulator: three
      visibly distinct rows (uniform gaps / space-between / bottom-aligned
      with a weighted filler). (Screenshot in the PR.)
- [ ] All §10 `commonTest` rows pass via `:shared:testDebugUnitTest` (and
      `:shared:allTests` on macOS).
- [ ] All §10 `androidApp` unit tests pass, including the row factory-bypass
      regression.
- [ ] Widget round-trip invariant (§6) holds: canonical models equal, layout
      defaults normalize to absent, `spacing` exact, idempotent.
- [ ] `weight` inside a row visibly claims leftover width; at the root it
      still throws (unit-tested).
- [ ] `grep -rE 'import (androidx|android|java)\.' shared/src/commonMain/`
      returns nothing.
- [ ] `./gradlew :androidApp:assembleDebug` and `:androidApp:lint` pass.

## Open questions (deliberately deferred)

- **`spacing` + positional pack** — Compose's `spacedBy(space, alignment)`
  can gap *and* center/end-pack simultaneously. Lifting the mutual exclusion
  for positional tokens only (`start|center|end`, never `space*`) is
  backward-compatible if wanted later.
- **Negative `spacing`** — legal in Compose (overlapping children; avatar
  stacks). Allowing it is a one-line validation change; deferred until a real
  payload wants it.
- **A horizontal `spacer`** — relax `spacer` to "at least one of
  `width`/`height`" (mirroring the `size` modifier's rule), or rely on
  `spacing`/`size`-modifier gaps indefinitely. Needs a SPEC-0002 §3 revision
  either way.
- **`Box`** — the remaining container; needs the 2-D alignment vocabulary and
  a per-child alignment story (Compose's `BoxScope.align` is a *modifier*,
  which would make it the first scoped modifier after `weight`).
- **Lazy lists** — `LazyRow`/`LazyColumn` share the vocabulary designed here
  but add item keys, content types, and the fold-memoization question
  SPEC-0005 parked.
