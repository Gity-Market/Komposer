# SPEC-0005 — Modifier System v1

**Status:** Proposed
**Depends on:** SPEC-0001 (wire conventions), SPEC-0002 (node catalog), SPEC-0003 (shared models & serializer), SPEC-0004 (rendering pipeline)
**Delivers:** roadmap Phase 3 — a widget's appearance controlled from JSON via a documented, versioned subset of modifiers

## Scope

A serializable, **ordered** representation of styling/layout that maps onto
Compose `Modifier`: the wire shape of the modifier list, a small curated v1
allow-list (padding, size, fill, background, weight), the column layout
vocabulary (`verticalArrangement` / `horizontalAlignment`), the shared Kotlin
models, and how the ordered list folds into a real `Modifier` on the Android
side. This spec also dissolves the two interim hardcodes SPEC-0002 flagged:
`column`'s `Modifier.fillMaxWidth()` and `spacer`'s `fillMaxWidth()`.

The governing constraint is stated up front: **order is semantics**.
`Modifier.padding(8.dp).background(c)` and `Modifier.background(c).padding(8.dp)`
draw different pixels, so the wire format must preserve order — everything else
in §1 follows from that.

## Non-goals

- **`clickable`.** The roadmap names it in the Phase 3 list; this spec moves it
  to Phase 5 with rationale in §2.6. Its wire name is reserved.
- Shapes — no `clip`, no `border`, no corner radius on `background`. A shape
  vocabulary (per-corner radii, circles) is its own design; see Open questions.
- `Arrangement.spacedBy` — a *parameterized* arrangement doesn't fit the
  closed-enum token convention; deferred (Open questions).
- `wrapContent*`, `requiredSize`, `offset`, `alpha`, `weight` for rows — the
  allow-list grows deliberately (§2.7); Row itself is Phase 4.
- Third-party/custom modifier types. Nodes are open (factory registry);
  modifiers are deliberately closed in v1 (§4).
- Animation, interaction states, semantics/accessibility modifiers.

---

## 1. Wire representation: the ordered `modifiers` list

Every node — present and future — accepts an optional `modifiers` field: a
JSON **array** of modifier objects, each discriminated by a `"type"` string
exactly like nodes are.

```json
{
  "type": "text",
  "text": "chip",
  "modifiers": [
    { "type": "background", "color": "#FFD54F" },
    { "type": "padding", "horizontal": 12, "vertical": 4 }
  ]
}
```

| Field | Type | Required | Meaning |
| --- | --- | --- | --- |
| `modifiers` | array of modifier objects | no (default `[]`) | Applied in array order, first element outermost |

**Rules:**

- **Array order is application order.** `"modifiers": [A, B, C]` means
  `Modifier.a().b().c()` — the example above is `background(...).padding(...)`,
  a yellow chip whose color includes the padding; reversing the array insets
  the color instead.
- **Repetition is legal.** `[padding, background, padding]` is meaningful in
  Compose and therefore meaningful on the wire. This alone rules out any
  bag/map shape.
- The list attaches to **every node uniformly**, enforced at the type level:
  `KomposerModel` itself declares the property (§4). Any node type is allowed
  to carry any modifier; the one context-dependent modifier (`weight`) fails at
  render time when misplaced, not at parse time (§2.5).
- **`modifiers` joins `type` as a reserved field name** (extends SPEC-0001 §2):
  no node may declare a wire field named `modifiers` for any other purpose.
- Modifier objects reuse the existing discriminator machinery: the serializer's
  `classDiscriminator = "type"` applies to every polymorphic hierarchy in the
  `Json` instance, so a modifier is `{"type": "padding", ...}` with no new
  convention. Modifier `type` tokens live in a **separate namespace** from node
  `type` tokens (different polymorphic bases — `KomposerModifier` vs
  `KomposerModel`), so a token collision between the two is technically
  impossible; we still avoid reusing node names for modifiers, for grep-ability.
- The reserved-key rule applies inside modifiers too: no modifier may declare a
  wire field named `type`.
- `"modifiers": []` parses identically to an absent field; the canonical
  encoding omits it (`encodeDefaults = false`, SPEC-0001 §4).

**Rejected alternatives** (each fails the order constraint or worse):

- *Property bag on the node* (`"padding": 8, "background": "#…"`): unordered,
  cannot express repetition, and every new modifier would collide with the
  node's own field namespace.
- *Wrapper nodes* (Flutter-style `{"type": "padding", "child": …}`): order
  becomes nesting depth, payloads get deep and noisy, and every modifier would
  ride through the factory registry, renderer `when`, and both visitors as a
  full node type — five touch-points per modifier instead of two (§2.7).
- *Per-node opt-in* (only some nodes get `modifiers`): nothing in the catalog
  is legitimately modifier-less (every Compose composable takes a `modifier`
  parameter by convention), and opt-in invites "forgot to add it" drift.

## 2. Modifier catalog v1

Seven wire types covering five capabilities. Scalar conventions are SPEC-0001
§3: dimensions are **dp** numbers, colors are `#RRGGBB`/`#AARRGGBB` strings,
enums are lowerCamelCase tokens. Validation for every rule below is `require`
in the model `init` (§7).

### 2.1 `padding`

Maps to the three `Modifier.padding` overloads. Exactly one **group** of fields
may be used per instance, mirroring Compose's overload set — Compose has no
"all plus start" overload, so the wire doesn't either.

| Field | Wire type | Group | Compose mapping |
| --- | --- | --- | --- |
| `all` | number (dp) ≥ 0 | A | `padding(all.dp)` |
| `horizontal` | number (dp) ≥ 0 | B | `padding(horizontal = h.dp, vertical = v.dp)` (absent axis ⇒ 0) |
| `vertical` | number (dp) ≥ 0 | B | 〃 |
| `start` / `top` / `end` / `bottom` | number (dp) ≥ 0 | C | `padding(start, top, end, bottom)` (absent edge ⇒ 0) |

- At least one field must be present; fields from more than one group fail
  parsing.
- All values must be finite and ≥ 0 (Compose rejects negative padding at
  runtime; we reject it at parse time).
- `{"type": "padding", "horizontal": 8}` and
  `{"type": "padding", "start": 8, "end": 8}` render identically but are
  **distinct models** and both round-trip exactly as written (§6) — no
  cross-group normalization.

### 2.2 `size`

Maps to `Modifier.width` / `height` / `size` (the constraint-respecting
variants, not `required*`).

| Field | Wire type | Required | Compose mapping |
| --- | --- | --- | --- |
| `width` | number (dp) ≥ 0 | at least one of the two | both set → `size(w.dp, h.dp)`; else `width(w.dp)` / `height(h.dp)` |
| `height` | number (dp) ≥ 0 | 〃 | 〃 |

Values must be finite and ≥ 0. Like Compose, `size` yields to incoming
constraints — it is not a guarantee, and this spec does not add `requiredSize`
(Open questions).

### 2.3 `fillMaxWidth` / `fillMaxHeight` / `fillMaxSize`

Three wire types, one per Compose function — mirroring Compose 1:1 beats a
single `fill` type with a direction enum (rejected: it invents vocabulary
Compose doesn't have, for zero wire savings).

| `type` | Field | Wire type | Default when absent | Compose mapping |
| --- | --- | --- | --- | --- |
| `fillMaxWidth` | `fraction` | number, `0 ≤ f ≤ 1` | `1` | `fillMaxWidth(fraction)` |
| `fillMaxHeight` | `fraction` | 〃 | `1` | `fillMaxHeight(fraction)` |
| `fillMaxSize` | `fraction` | 〃 | `1` | `fillMaxSize(fraction)` |

`{"type": "fillMaxWidth"}` with no fields is the common case and is legal.
The `0 ≤ f ≤ 1` range is *our* wire rule (Compose doesn't validate; values
above 1 just get coerced by constraints — we'd rather fail a nonsensical
payload loudly).

### 2.4 `background`

Maps to `Modifier.background(color)`. Solid color only in v1; shape is a
deferred vocabulary (Open questions).

| Field | Wire type | Required | Compose mapping |
| --- | --- | --- | --- |
| `color` | color string (SPEC-0001 §3) | **yes** | `background(parseKomposerColor(color))` |

### 2.5 `weight`

Maps to `ColumnScope.weight` (and `RowScope.weight` when Row arrives in
Phase 4).

| Field | Wire type | Required | Default when absent | Compose mapping |
| --- | --- | --- | --- | --- |
| `value` | number > 0 | **yes** | — | `weight(value, fill)` |
| `fill` | boolean | no | `true` | 〃 |

(The field is `value`, not `weight` — `{"type": "weight", "weight": 1}`
stutters.)

**Scoping, honestly.** `weight` only means something on a direct child of a
`Column` (or, later, `Row`). A model's `init` block cannot see the tree it sits
in, so this **cannot be a parse-time rule**. The decision:

- Parsing accepts `weight` anywhere.
- Rendering **fails loudly** — `KomposerRenderException` — when a `weight`
  modifier is folded without an enclosing weight-capable scope (§5.3): at the
  document root, or on any node whose parent doesn't provide one.
- Silently ignoring it was rejected outright: dropping UI semantics without a
  sound is exactly what SPEC-0001 §5's strictness policy exists to prevent.
- A pre-render validation pass over the model tree (a `KomposerModelVisitor` —
  finally a real server-side job for it) could front-run the render failure;
  deferred as an Open question, not required for v1.

Compose treats weight as parent data, so its position in the chain doesn't
change layout — the fold still applies it in list position (§5.2), because one
uniform rule beats a documented exception.

### 2.6 `clickable` — deferred to Phase 5, name reserved

The roadmap listed `clickable` in this phase. It is **moved to Phase 5**:

- A `clickable` with no action payload renders a ripple that does nothing —
  the wire would *promise* interactivity the client doesn't deliver. A payload
  that lies is worse than a missing feature.
- A `clickable` with an action payload ("navigate", "emit event `X`") *is* the
  Phase 5 design — the action vocabulary, dispatch API, and host-app callback
  registration are its core questions. Deciding them inside a modifier spec
  would pre-empt Phase 5 exactly the way SPEC-0002 refused to pre-empt this
  spec ("nothing here may pre-empt it").
- Scoping it down to an opaque `actionId` string was considered and rejected:
  it still forces the dispatch API question (how does the host app receive the
  id?), which is the hard part — the modifier itself is trivial once actions
  exist.

The wire token `clickable` is **reserved** for that future modifier: nothing
else may claim the name in the meantime.

### 2.7 Adding a modifier type (growth checklist)

The allow-list grows deliberately. A new modifier touches exactly **three**
places — deliberately fewer than a node's five:

1. A `@Serializable @SerialName("…")` data class implementing
   `KomposerModifier` in `shared/commonMain/…/core/model/modifier/`, with
   `require`-based validation (§4). Sealing means there is no schema/registry
   step.
2. A branch in the fold's `when` (§5.2) — the compiler **forces** this one:
   the `when` is exhaustive over a sealed type with no `else`.
3. A row in this spec (or its successor): fields, defaults, validation,
   Compose mapping.

## 3. Column layout vocabulary

`verticalArrangement` and `horizontalAlignment` land now so layout vocabulary
is designed once — but note they are **node fields on `column`**, not
modifiers: in Compose they are `Column` parameters, not `Modifier` calls, and
the wire mirrors Compose's own split.

### Wire fields (added to `column`, SPEC-0002 §2)

| Field | Wire type | Required | Default when absent | Compose mapping |
| --- | --- | --- | --- | --- |
| `verticalArrangement` | enum token (below) | no | `Arrangement.Top` | `verticalArrangement` |
| `horizontalAlignment` | enum token (below) | no | `Alignment.Start` | `horizontalAlignment` |

### Token sets (closed enums)

| `verticalArrangement` token | Compose value |
| --- | --- |
| `top` | `Arrangement.Top` |
| `center` | `Arrangement.Center` |
| `bottom` | `Arrangement.Bottom` |
| `spaceBetween` | `Arrangement.SpaceBetween` |
| `spaceAround` | `Arrangement.SpaceAround` |
| `spaceEvenly` | `Arrangement.SpaceEvenly` |

| `horizontalAlignment` token | Compose value |
| --- | --- |
| `start` | `Alignment.Start` |
| `center` | `Alignment.CenterHorizontally` |
| `end` | `Alignment.End` |

**The "designed once" contract** for Phase 4's Row (and Box): alignment and
arrangement tokens are **per-axis vocabularies**. Horizontal-axis positions are
`start`/`center`/`end`; vertical-axis positions are `top`/`center`/`bottom`;
main-axis distribution adds `spaceBetween`/`spaceAround`/`spaceEvenly`. Row's
`horizontalArrangement` will be `start|center|end|spaceBetween|spaceAround|spaceEvenly`
and its `verticalAlignment` will be `top|center|bottom` — same tokens, same
package (§4), new enum classes (Compose's `Arrangement.Horizontal` and
`Arrangement.Vertical` are distinct types; pretending one enum covers both
would push the axis mismatch to render time).

`Arrangement.spacedBy(dp)` is deliberately absent: it takes a parameter, so it
cannot be a closed-enum token. It is the most-wanted omission and is tracked in
Open questions with candidate shapes.

## 4. Kotlin models (`shared/commonMain`)

### Layout

```
shared/src/commonMain/kotlin/ir/gity/komposer/core/model/
├── WireColor.kt                  # color regex, promoted out of TextModel (below)
├── layout/
│   └── LayoutValues.kt           # VerticalArrangementValue, HorizontalAlignmentValue
└── modifier/
    ├── KomposerModifier.kt       # the sealed interface
    ├── PaddingModifier.kt
    ├── SizeModifier.kt
    ├── FillModifiers.kt          # FillMaxWidth/Height/SizeModifier
    ├── BackgroundModifier.kt
    └── WeightModifier.kt
```

### The sealed base — and why it differs from `KomposerModel`

```kotlin
// commonMain — no Compose, no Android, no java.* imports (SPEC-0003 §2 rules apply).
// Not @Serializable: interfaces carry no serializer; kotlinx.serialization treats a
// sealed interface like a sealed class — properties typed KomposerModifier serialize
// with closed polymorphism and the configured "type" discriminator, no registration.
sealed interface KomposerModifier
```

`KomposerModel` is a **non-sealed** interface registered in `KomposerSchema` —
third-party nodes plug in via schema + factory registry. Modifiers get the
opposite call, **sealed**, deliberately:

- There is no per-modifier factory abstraction: the only consumer is the fold's
  `when` (§5.2). Sealing makes that `when` compiler-checked — adding a modifier
  type without a fold branch is a **compile error**, strictly better than
  SPEC-0004's else-throws (which exists because the renderer dispatches over an
  open interface and has no choice).
- Sealing *is* the registration: no `KomposerSchema` entry, no chance of the
  "registered on the wire but unknown to the fold" drift an open hierarchy
  invites. `KomposerSchema` gains only a comment pointing here.
- The closed hierarchy is the honest type-level statement of "a small curated
  allow-list, grown deliberately." If third-party modifiers ever matter,
  unsealing plus a fold registry is Phase 4's registration problem (Open
  questions).
- No `accept(visitor)` on modifiers: traversal happens through nodes; a
  modifier visitor can arrive when a real pass needs one.

Unknown modifier `type` on the wire ⇒ `SerializationException` from the sealed
serializer ⇒ wrapped into `KomposerParseException` by the existing SPEC-0003 §5
machinery. Strictness comes for free.

### The modifier classes

```kotlin
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

@Serializable
@SerialName("size")
data class SizeModifier(
    val width: Float? = null,
    val height: Float? = null,
) : KomposerModifier {
    init {
        require(width != null || height != null) { "size requires width and/or height" }
        listOfNotNull(width, height).forEach {
            require(it.isFinite() && it >= 0f) { "size values must be finite and >= 0, was $it" }
        }
    }
}

@Serializable
@SerialName("fillMaxWidth")
data class FillMaxWidthModifier(val fraction: Float? = null) : KomposerModifier {
    init { fraction?.let { require(it.isFinite() && it in 0f..1f) { "fraction must be in 0..1, was $it" } } }
}
// FillMaxHeightModifier ("fillMaxHeight"), FillMaxSizeModifier ("fillMaxSize"): same shape.

@Serializable
@SerialName("background")
data class BackgroundModifier(val color: String) : KomposerModifier {
    init {
        require(WireColor.REGEX.matches(color)) {
            "color must match #RRGGBB or #AARRGGBB, was \"$color\""
        }
    }
}

@Serializable
@SerialName("weight")
data class WeightModifier(
    val value: Float,
    val fill: Boolean? = null,
) : KomposerModifier {
    init { require(value.isFinite() && value > 0f) { "weight value must be > 0, was $value" } }
}
```

`WireColor` promotes the color regex out of `TextModel.Companion` — two models
now need it, and a wire-format constant doesn't belong to one node:

```kotlin
// core/model/WireColor.kt
object WireColor {
    val REGEX = Regex("^#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")
}
```

`TextModel.COLOR_REGEX` is removed; its `init` uses `WireColor.REGEX`
(Migration notes).

### `modifiers` on every node — the interface carries it

```kotlin
interface KomposerModel {
    val modifiers: List<KomposerModifier>
    fun accept(visitor: KomposerModelVisitor)
}
```

Every model overrides it as its **last constructor parameter**, defaulting to
empty (last, so existing positional construction sites keep compiling):

```kotlin
@Serializable
@SerialName("spacer")
data class SpacerModel(
    val height: Float,
    override val modifiers: List<KomposerModifier> = emptyList(),
) : KomposerModel { /* init unchanged */ }
```

`TextModel` gains the same parameter. `ColumnModel` gains it plus the §3
fields:

```kotlin
@Serializable
@SerialName("column")
data class ColumnModel(
    val children: List<KomposerModel> = emptyList(),
    val verticalArrangement: VerticalArrangementValue? = null,
    val horizontalAlignment: HorizontalAlignmentValue? = null,
    override val modifiers: List<KomposerModifier> = emptyList(),
) : KomposerModel
```

The layout enums follow the SPEC-0002 enum pattern exactly (`@Serializable`
enum class, entry-level `@SerialName` wire tokens), in `core/model/layout/` so
Phase 4's Row enums land beside them:

```kotlin
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
```

Interface-property-vs-base-class: an abstract base class was rejected (it
would burn the single supertype slot and forbid data-class hierarchies richer
than this one for no gain); a convention without the interface property was
rejected because §5's generic fold needs `widget.modifiers` to exist on every
widget, and the compiler should enforce the wire promise "every node accepts
modifiers", not review.

## 5. Mapping to Compose (`androidApp`)

### 5.1 Widgets carry the model list, verbatim

`KomposerWidget` gains the same property:

```kotlin
interface KomposerWidget {
    val modifiers: List<KomposerModifier>
    fun toModel(): KomposerModel
    fun Accept(visitor: KomposerWidgetVisitor)
}
```

Each widget adds `override val modifiers: List<KomposerModifier> = emptyList()`
to its constructor; each factory **copies the list through unchanged**
(`modifiers = model.modifiers`) — the factory's whole modifier job is a
faithful copy.

This is the load-bearing decision of the Android side, so the alternatives are
named:

- *Fold in the factory, store a `Modifier` on the widget* — rejected twice
  over. A folded `Modifier` is a black box: `toModel()` could never recover
  the wire data, breaking SPEC-0004 §4's round-trip invariant. And `weight`
  cannot be folded early at all: `ColumnScope` exists only inside the
  `Column` content lambda at composition time (§5.3).
- *A parallel widget-side modifier hierarchy with Compose types
  (`Dp`, `Color`)* — rejected. `TextWidget` stores Compose types because
  Compose `Text` takes them as individually typed parameters; `Modifier` is
  opaque, so the model list already *is* the richest inspectable
  representation. Duplicating seven data classes buys a second mapping layer,
  plus the color/dp canonicalization headaches of SPEC-0004 §4, for zero
  information gain.

Consequence worth stating: `toModel()` on modifiers is **identity**, so the
round-trip is exact for *every* modifier list, not just canonical ones (§6).

### 5.2 The fold — one exhaustive `when`, order preserved

```kotlin
// core/renderer/KomposerModifierFold.kt
fun List<KomposerModifier>.toComposeModifier(scope: KomposerRenderScope? = null): Modifier =
    fold<KomposerModifier, Modifier>(Modifier) { acc, modifier ->
        when (modifier) {
            is PaddingModifier -> acc.applyPadding(modifier)
            is SizeModifier -> when {
                modifier.width != null && modifier.height != null ->
                    acc.size(modifier.width.dp, modifier.height.dp)
                modifier.width != null -> acc.width(modifier.width.dp)
                else -> acc.height(modifier.height!!.dp)
            }
            is FillMaxWidthModifier -> acc.fillMaxWidth(modifier.fraction ?: 1f)
            is FillMaxHeightModifier -> acc.fillMaxHeight(modifier.fraction ?: 1f)
            is FillMaxSizeModifier -> acc.fillMaxSize(modifier.fraction ?: 1f)
            is BackgroundModifier -> acc.background(parseKomposerColor(modifier.color))
            is WeightModifier -> scope?.weight(acc, modifier.value, modifier.fill ?: true)
                ?: throw KomposerRenderException(
                    "weight modifier requires a Column (or Row) parent"
                )
        }
    }

private fun Modifier.applyPadding(m: PaddingModifier): Modifier = when {
    m.all != null -> padding(m.all.dp)
    m.horizontal != null || m.vertical != null ->
        padding(horizontal = (m.horizontal ?: 0f).dp, vertical = (m.vertical ?: 0f).dp)
    else -> padding(
        start = (m.start ?: 0f).dp, top = (m.top ?: 0f).dp,
        end = (m.end ?: 0f).dp, bottom = (m.bottom ?: 0f).dp,
    )
}
```

- Left fold ⇒ list order = chain order, the §1 guarantee.
- **No `else` branch, deliberately** — the sealed hierarchy makes the `when`
  exhaustive, so §2.7's "the compiler forces the fold branch" holds. Contrast
  with `KomposerRenderer`'s else-throws, which is stuck with an open interface.
- `parseKomposerColor` is reused from SPEC-0004 §3 — same parsing, same
  `KomposerRenderException` on the (unreachable, thanks to `init` validation)
  bad-color path.
- The fold is pure and non-composable: it runs anywhere, tests included.
  It re-runs per composition; that's a handful of small allocations per node
  and fine for v1 (memoization: Open questions).

### 5.3 `weight` needs a scope: `KomposerRenderScope`

`Modifier.weight` is a member of `ColumnScope`/`RowScope`, and those receivers
exist only inside the parent's content lambda. So the *parent's renderer*
hands its scope down one level:

```kotlin
// core/renderer/KomposerRenderScope.kt
interface KomposerRenderScope {
    fun weight(modifier: Modifier, value: Float, fill: Boolean): Modifier
}

class ColumnRenderScope(private val scope: ColumnScope) : KomposerRenderScope {
    override fun weight(modifier: Modifier, value: Float, fill: Boolean): Modifier =
        with(scope) { modifier.weight(value, fill) }
}
```

`KomposerRenderer` and every `Render*` gain a scope parameter, `null` at the
root:

```kotlin
@Composable
fun KomposerRenderer(widget: KomposerWidget, scope: KomposerRenderScope? = null) {
    when (widget) {
        is ColumnWidget -> RenderColumn(widget, scope)
        is TextWidget -> RenderText(widget, scope)
        is SpacerWidget -> RenderSpacer(widget, scope)
        else -> throw KomposerRenderException("No render branch for ${widget::class.simpleName}")
    }
}
```

A widget folds **its own** modifiers with the scope **it received** (its
parent's), and a composite creates the scope for its children:

```kotlin
@Composable
fun RenderColumn(widget: ColumnWidget, scope: KomposerRenderScope?) {
    Column(
        modifier = widget.modifiers.toComposeModifier(scope),
        verticalArrangement = widget.verticalArrangement,
        horizontalAlignment = widget.horizontalAlignment,
    ) {
        val childScope = ColumnRenderScope(this)
        widget.getChildren().forEach { child -> KomposerRenderer(child, childScope) }
    }
}
```

`weight` at the document root (or under a future scope-less composite like Box)
folds with `scope = null` and throws — the §2.5 failure mode. Phase 4's Row
adds a `RowRenderScope`; nothing else changes.

### 5.4 Node-intrinsic modifiers: wire first, intrinsic appended

Some nodes have intrinsic modifier needs (`spacer`'s `height`). Rule, matching
the Compose component convention that the caller's `modifier` parameter heads
the chain and internals append to it:

> Wire modifiers fold first; node-intrinsic modifiers are appended after.

```kotlin
@Composable
fun RenderSpacer(widget: SpacerWidget, scope: KomposerRenderScope?) {
    Spacer(modifier = widget.modifiers.toComposeModifier(scope).height(widget.height))
}
```

Consequence (identical to Compose semantics, so we inherit rather than invent):
an earlier `size`/`height` wire modifier constrains first and therefore wins
over the intrinsic `height` — a payload can override a spacer's height with a
modifier, exactly as a Compose caller can.

### 5.5 The hardcodes dissolve

- **`RenderColumn` loses `Modifier.fillMaxWidth()`** (the SPEC-0002 §2 interim
  hardcode). A bare `column` now sizes like a bare Compose `Column`: wrapping
  its content. Payloads that relied on full width say so:
  `"modifiers": [{"type": "fillMaxWidth"}]`. This is a **visible behavior
  change** — see Migration notes.
- **`RenderSpacer` loses `fillMaxWidth()`** too. Inside a column the vertical
  gap is unchanged (height is what matters and an invisible node's width isn't
  observable there); the spacer keeps only its intrinsic `height`.
- **`TextWidget.modifier: Modifier` is deleted.** It was never set by the
  factory and never reached the wire; now that a real modifier path exists, an
  opaque second path that `toModel()` silently drops is exactly the
  "two competing paths" bug class SPEC-0003 §2 killed for construction. The
  other widget-only fields (`style`, `fontFamily`, `onTextLayout`) stay: they
  have *no* wire counterpart yet, so they compete with nothing. Anything only
  expressible via a raw `Modifier` is, by definition, pressure to grow the §2
  allow-list — an escape hatch would hide that signal.

### 5.6 Column fields on the widget

`ColumnWidget` follows the `TextWidget` pattern — Compose-typed storage,
defaults matching Compose:

```kotlin
class ColumnWidget(
    children: MutableList<KomposerWidget> = mutableListOf(),
    val verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    val horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    override val modifiers: List<KomposerModifier> = emptyList(),
) : KomposerWidget, KomposerCompositeWidget { … }
```

`ColumnWidgetFactory` maps the enums with exhaustive `when`s (the
`TextWidgetFactory` pattern); `toModel()` normalizes Compose defaults back to
absent per §6.

## 6. Round-trip & canonical form

Extends SPEC-0004 §4's invariant. Two regimes, because the two storage
strategies differ:

**Modifiers: exact for everything.** Widgets store the model list verbatim
(§5.1), so `create(m).toModel()` preserves `modifiers` **exactly — for every
list, canonical or not**. There is nothing to normalize because nothing is
lossy: SPEC-0004's canonical-form machinery exists solely to absorb lossy
Compose-typed widget storage, and modifiers don't have any. Explicitly writing
a client-side default (`"fill": true`, `"fraction": 1`) or an uncompacted
padding spelling (`start`/`end` instead of `horizontal`) is non-canonical *on
the wire* in the SPEC-0002 sense but round-trips to itself unchanged. A
modifier list is canonical as written.

**Column layout fields: normalized like text fields.** Compose-typed storage
(§5.6) absorbs explicit defaults, so `toModel()` normalizes:

| Widget value | Model field becomes |
| --- | --- |
| `Arrangement.Top` | `null` |
| `Arrangement.Center` / `Bottom` / `SpaceBetween` / `SpaceAround` / `SpaceEvenly` | the matching token |
| any other `Arrangement.Vertical` (e.g. hand-built `spacedBy`) | `null` — unrepresentable in v1, collapses to absent (same policy as `TextAlign`'s `else -> null` in SPEC-0004) |
| `Alignment.Start` | `null` |
| `Alignment.CenterHorizontally` / `End` | the matching token |
| any other `Alignment.Horizontal` | `null` |

**Invariant (tested):** for every canonical model `m` — where canonical now
additionally requires `verticalArrangement`/`horizontalAlignment` to be absent
or non-default, and places **no constraint** on `modifiers` —
`registry.build().create(m).toModel() == m`, including deep equality of the
modifier lists; normalization stays idempotent. Together with SPEC-0001 §6,
`JSON → model → widget → model → JSON` remains lossless for canonical v1
payloads, now with modifiers aboard.

## 7. Validation

All rules are `require` in model `init` blocks (pure Kotlin, runs in
`commonMain`, fails at parse time wrapped into `KomposerParseException` —
SPEC-0002 §4 / SPEC-0003 §5 machinery, unchanged). Never `check`.

| Model | Rule |
| --- | --- |
| `padding` | at least one field present |
| `padding` | fields from exactly one group: `all` ⊕ (`horizontal`/`vertical`) ⊕ (`start`/`top`/`end`/`bottom`) |
| `padding` | every present value finite and ≥ 0 |
| `size` | at least one of `width`/`height` present |
| `size` | every present value finite and ≥ 0 |
| `fillMaxWidth`/`fillMaxHeight`/`fillMaxSize` | `fraction` finite and in `0..1` when present |
| `background` | `color` matches `WireColor.REGEX` |
| `weight` | `value` finite and > 0 |

Enum tokens (`verticalArrangement`, `horizontalAlignment`) and modifier `type`
discrimination are validated by serialization itself, as everywhere else.
`weight` placement is render-time by design (§2.5).

## 8. Strictness additions (extends SPEC-0001 §5)

| Situation | Behavior |
| --- | --- |
| Unknown modifier `type` | **Fail** (`KomposerParseException`) — same policy as unknown node types, same rationale |
| Unknown *field* on a known modifier | **Ignore** (`ignoreUnknownKeys = true`, global) |
| `"modifiers": []` | **Accept** — identical to absent; canonical encoding omits it |
| `"modifiers": null` | **Fail** — the property is a non-nullable list with a default. This matches the *existing* behavior of `column.children: null` and clarifies SPEC-0001 §5's "explicit `null` accepted" row: that rule covers **nullable optional fields**; list-valued fields with defaults must be omitted, not nulled |
| `weight` outside a Column | Parses fine; **fails at render** with `KomposerRenderException` (§2.5) |
| Bad modifier values | **Fail** at parse (§7) |

## 9. Reference payload

Exercises every v1 modifier except `fillMaxHeight`, both §3 fields, order
sensitivity, and the weight scope. Replaces the SPEC-0001 §7 payload as
`MainActivity`'s demo JSON (that payload remains valid and remains SPEC-0001's
wire-format reference; this one is the Phase 3 acceptance surface):

```json
{
  "version": 1,
  "root": {
    "type": "column",
    "modifiers": [
      { "type": "fillMaxSize" },
      { "type": "background", "color": "#F2F2F7" },
      { "type": "padding", "all": 16 }
    ],
    "horizontalAlignment": "center",
    "children": [
      { "type": "text", "text": "Hello Komposer, modified", "fontWeight": 700, "fontSize": 20, "color": "#6200EE" },
      { "type": "spacer", "height": 12 },
      {
        "type": "text",
        "text": "background → padding: the yellow includes this inset",
        "modifiers": [
          { "type": "background", "color": "#FFD54F" },
          { "type": "padding", "horizontal": 12, "vertical": 4 }
        ]
      },
      {
        "type": "text",
        "text": "padding → background: the yellow hugs the text",
        "modifiers": [
          { "type": "padding", "horizontal": 12, "vertical": 4 },
          { "type": "background", "color": "#FFD54F" }
        ]
      },
      { "type": "spacer", "height": 12 },
      {
        "type": "text",
        "text": "weighted: fills the leftover vertical space",
        "modifiers": [
          { "type": "weight", "value": 1 },
          { "type": "fillMaxWidth" },
          { "type": "background", "color": "#E1F5FE" }
        ]
      },
      {
        "type": "column",
        "modifiers": [
          { "type": "fillMaxWidth", "fraction": 0.5 },
          { "type": "size", "height": 120 },
          { "type": "background", "color": "#EDE7F6" },
          { "type": "padding", "all": 8 }
        ],
        "verticalArrangement": "spaceBetween",
        "children": [
          { "type": "text", "text": "half width, 120dp tall", "fontStyle": "italic" },
          { "type": "text", "text": "spaceBetween pushes me down", "maxLines": 1, "overflow": "ellipsis" }
        ]
      }
    ]
  }
}
```

## 10. Tests

`commonTest` (runs on all KMP targets):

| Test | Asserts |
| --- | --- |
| Modifier round-trip | each modifier type, minimal + fully-populated: `parseNode(encodeNode(m)) == m` via a carrier node |
| Order & repetition | a node with `[padding, background, padding]` round-trips with order and duplicates intact |
| Column layout round-trip | both §3 fields, each token |
| §9 payload | parses; tree asserted field-by-field; re-encodes and re-parses equal |
| Each §7 rule | bad payload → `KomposerParseException` |
| Unknown modifier type | `{"type": "blink"}` in a modifier list → `KomposerParseException` |
| `"modifiers": null` | fails; `"modifiers": []` equals absent |
| Encoding minimality | empty list and default fields omitted from encoding |

`androidApp` unit tests (JVM — `Modifier.Element` implementations define
structural equality, required for recomposition skipping, so folded chains
compare with `==`):

| Test | Asserts |
| --- | --- |
| Fold order | `fold([background, padding])` equals the hand-built `Modifier.background(...).padding(...)` and differs from the reversed fold |
| Padding overloads | each §2.1 group dispatches to the matching Compose overload |
| Weight scope | fold with `scope = null` throws `KomposerRenderException`; a stub scope receives `(value, fill)` in list position |
| Widget round-trip | `ToModelRoundTripTest` extended: arbitrary modifier lists survive `create(m).toModel()` exactly; explicit `"verticalArrangement": "top"` normalizes to absent; idempotence holds |

## Migration notes

Breaking or visible; one commit ideally, shared first, then `androidApp`:

1. **`KomposerModel` gains `val modifiers`** — every implementor overrides it
   (last constructor parameter, default `emptyList()`). Third-party models (if
   any exist yet) break loudly at compile time.
2. **`KomposerWidget` gains `val modifiers`** — `TextWidget`, `ColumnWidget`,
   `SpacerWidget` add the parameter; every factory copies `model.modifiers`
   through.
3. **`TextWidget.modifier: Modifier` is deleted** (§5.5); `RenderText` passes
   the folded list instead. Hand-built widget trees styling via that field
   switch to modifier models.
4. **`RenderColumn` drops `fillMaxWidth()`** — *visible change*: columns in
   existing payloads (including SPEC-0001 §7's) become wrap-content-width.
   Layout of the current reference content is unaffected in practice (its
   texts wrap at the same incoming constraints), but any payload relying on
   full-width columns must add `{"type": "fillMaxWidth"}`. SPEC-0002 §2's
   render note is superseded by this spec.
5. **`RenderSpacer` drops `fillMaxWidth()`** — no visible change inside
   columns; SPEC-0002 §3's render note is superseded.
6. **`KomposerRenderer` and all `Render*` functions gain a
   `scope: KomposerRenderScope?` parameter** (root callers pass nothing —
   it defaults to `null`).
7. **`TextModel.COLOR_REGEX` moves to `WireColor.REGEX`**
   (`core/model/WireColor.kt`); `TextModel`'s `init` and any test referencing
   the companion update.
8. **`KomposerSchema` is unchanged** — sealed modifiers self-register; a
   comment in the schema records why there is no `polymorphic(KomposerModifier)`
   block, so nobody "fixes" its absence.
9. `MainActivity`'s `REFERENCE_JSON` is replaced by §9's payload;
   `KomposerModelDemo` may optionally add a modifier to prove the hand-built
   path.
10. Visitors (`KomposerWidgetVisitor`, `KomposerModelVisitor`, `GraphBuilder`)
    are **untouched**: modifiers are node *data*, not nodes.
11. `specs/README.md` and `ROADMAP.md` link this spec; the roadmap's Phase 3
    allow-list note records `clickable`'s move to Phase 5 (§2.6).

## Acceptance criteria

- [ ] The §9 reference payload parses and renders on a device/emulator with
      every modifier visibly applied — including the two order-contrast rows
      rendering differently. (Screenshot in the PR.)
- [ ] A unit test proves order significance without a device:
      `fold([background, padding]) != fold([padding, background])`, and each
      equals its hand-built Compose chain.
- [ ] All §10 `commonTest` rows pass via `:shared:testDebugUnitTest` (and
      `:shared:allTests` on macOS).
- [ ] Widget round-trip stays exact: models with arbitrary modifier lists and
      canonical column fields satisfy `create(m).toModel() == m`; explicit
      layout defaults normalize to absent; normalization is idempotent.
- [ ] Every §7 validation rule has a failing-payload test; an unknown modifier
      `type` fails with `KomposerParseException`.
- [ ] `weight` inside a column visibly claims the leftover space; folding a
      `weight` with no scope throws `KomposerRenderException` (unit-tested).
- [ ] No `fillMaxWidth` hardcode remains in `RenderColumn` or `RenderSpacer`.
- [ ] `grep -rE 'import (androidx|android|java)\.' shared/src/commonMain/`
      still returns nothing.
- [ ] `./gradlew :androidApp:assembleDebug` and `:androidApp:lint` pass.

## Open questions (deliberately deferred)

- **`clickable` and the action vocabulary** — Phase 5; the wire token is
  reserved (§2.6). The modifier will be one line once actions exist.
- **`Arrangement.spacedBy`** — the most-wanted layout omission. Needs a
  parameterized-token design; candidates: a sibling `spacing` dp field on
  `column` that is mutually exclusive with `verticalArrangement`, or promoting
  arrangement from a token to an object. Decide when Row lands (same vocabulary,
  Phase 4).
- **Shape vocabulary** — corner radii for `background`, `clip`, `border`.
  One design, several consumers; don't ship it piecemeal.
- **Allow-list growth** — `wrapContent*`, `requiredSize`, `offset`, `alpha`,
  `aspectRatio` are the likely next entries, via the §2.7 checklist.
- **Pre-render `weight` placement validation** — a `KomposerModelVisitor` pass
  flagging misplaced weights at parse time instead of render time; also the
  first real server-side use of the model visitor.
- **Modifier extensibility** — unsealing plus a fold registry, if third-party
  modifiers ever matter; belongs to Phase 4's "one place to register"
  problem.
- **Fold memoization** — folds re-run per composition; cheap today, revisit
  with lazy lists (Phase 4).
- **Theme color references** (`"primary"`) — carried from SPEC-0002; applies
  to `background.color` identically.
