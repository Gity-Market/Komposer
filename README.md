# Komposer

> **Server-driven UI for Jetpack Compose — Kotlin-first, for Kotlin everywhere.**

Komposer drives native UI from data. A backend sends a serializable description
of a screen; the client deserializes it into a **Model** tree, maps that into a
tree of **Widgets**, and renders those widgets as real Jetpack Compose
`@Composable`s.

The name is deliberate: this is the **Compose** successor to our production
**XML-based** server-driven UI. The proven idea — ship UI as data, change
screens without shipping an app — stays; the client moves to a modern
declarative toolkit, and the UI contract moves into **Kotlin Multiplatform**,
so a Kotlin backend can construct screens using the *very same types* the
client renders.

---

## Why

- **Ship UI without shipping an app.** Layout, ordering, copy, and styling
  become data the server controls. No store release to change a screen.
- **One source of truth.** The Model schema is plain Kotlin in a shared
  module. The backend emits the same types the client parses — no drift
  between a hand-written JSON contract and a hand-written client parser,
  which is exactly where our XML system accumulated scar tissue.
- **Compose-native rendering.** The output is ordinary Compose — not a
  webview, not a custom canvas — so it themes, recomposes, previews, and
  profiles like any other Compose UI.
- **As little architecture as the job needs.** The codebase started as a
  deliberate GoF pattern study (Composite, Factory, Visitor). Phase 4 concluded
  the study: the patterns cost more in ceremony than they bought in flexibility
  against a closed node catalog, so they were replaced with sealed hierarchies
  and exhaustive `when` — the same guarantees, checked by the compiler.

## Scoping principle

Mapping the *entire* Compose API surface (every modifier, every parameter)
onto JSON is the known hard problem, and it is deliberately deferred. Instead,
one node — **Text** — gets a genuinely rich attribute set (`maxLines`,
`fontWeight`, `color`, `overflow`, …) to prove the wire format against a real
composable, while everything else stays minimal until the
[modifier problem](ROADMAP.md#phase-3--the-modifier-problem-the-hard-one) is
tackled on its own terms — now implemented (Phase 3).

---

## The pipeline

```
                    ┌────────────── encode() back to JSON ──────────────┐
                    │                                                    │
 JSON ───parse────► KomposerModel ──toWidget()─► KomposerWidget ───KomposerRenderer───► @Composable
 (wire,             (@Serializable,  (one        (Compose-aware        (exhaustive
  versioned)         sealed)          exhaustive  sealed value)         when — no else)
                          ▲           when)             │
                          └───────── toModel() ◄────────┘
```

- **`KomposerModel`** — the serializable shape of a node: a `@Serializable`
  **sealed** interface. Pure data; the KMP citizen the backend will share.
  Sealing *is* the registration — the compiler plugin emits a closed
  polymorphic serializer, so there is no schema object to keep in sync.
- **`KomposerWidget`** — the in-memory, Compose-aware node, also sealed. Its
  whole job is Compose-typed storage plus `toModel()`.
- **`KomposerModel.toWidget()`** — the *only* Model → Widget path: one
  exhaustive `when`, with per-node extensions carrying the mapping. No registry,
  so there is nothing to register and nothing to bypass.
- **`KomposerRenderer`** — the single dispatch point from a Widget tree into
  Compose; exhaustive over the sealed widgets, so a node without a render branch
  will not compile.

Six node types exist today: **Text**, **Image**, **Spacer**, and the three containers
**Column**, **Row**, **Box**.

## What replaced the patterns

Phase 4 traded the GoF layer for language features. The guarantees each pattern
was there to provide all survive — as compile errors instead of conventions:

| Was | Is now | Guarantee |
| --- | --- | --- |
| **Composite** — `KomposerCompositeWidget` (`addChild`/`getChildren`) | `ColumnWidget.children: List<KomposerWidget>`, an immutable value | Nothing mutates a widget tree after construction, so the round-trip law is easier to trust. |
| **Factory** — `KomposerWidgetFactory` + `FactoryRegistry` | `KomposerModel.toWidget()`, one exhaustive `when` | A node without a mapping branch does not compile. No registry ⇒ the nested-child bypass bug is structurally impossible. |
| **Visitor** ×2 — `KomposerModelVisitor`, `KomposerWidgetVisitor`/`GraphBuilder` | `debugGraph()`, a plain recursive function | The model visitor had zero implementors; `GraphBuilder`'s own dispatch `when` was already the pattern fighting the language. |
| **Manual registration** — `KomposerSchema` | `@Serializable sealed interface` | Sealing *is* the registration; nothing can be "known on the wire but unknown to the renderer". |
| **Specification** — `NonEmptyTextSpecification` in `NiceToHave.kt` | deleted with the rest of the scratchpad | Predicate-style validation never grew combinators. If pre-render validation is wanted, it returns as a plain recursive function over the sealed models — see roadmap Phase 4. |

---

## Status: what works, what's stubbed

This is early, exploratory code, and honesty about the seams is a feature.

**✅ Working — the JSON path (Phases 1–2).**
`KomposerJson2ModelDemo()` in `MainActivity.kt` is the primary demo: a raw JSON
document (the reference payload) is parsed by `DefaultKomposerSerializer`, built
into a widget tree by `toWidget()`, and rendered as styled pixels.
`JSON → Model → Widget → Model → JSON` is lossless for canonical v1 payloads,
tested in `commonTest` and `androidApp` unit tests.

**✅ Working — the in-memory path.** `KomposerModelDemo()` builds a
`ColumnModel` by hand, runs it through the same `toWidget()`, walks it with
`debugGraph()` for a debug dump, and renders it. Kept as an `@Preview`.

**✅ Multiplatform where it counts.** The model + serialization layer lives in
`shared/src/commonMain` — pure Kotlin, no Compose/Android/`java.*` imports —
and compiles for Android and the three iOS targets. Compose-typed code
(widgets, mapping, renderer) stays in `androidApp` by design until
the rendering story goes multiplatform. One caveat: a Kotlin *backend* sharing
the types still needs a `jvm()` target on `shared`, a one-line build change
deliberately deferred to
[roadmap Phase 6](ROADMAP.md#phase-6--backend--tooling).

**✅ Working — a compiler-checked architecture (Phase 4).** Both models and
widgets are sealed; the schema object, both visitors, the factory layer, and the
composite interface are deleted; `toWidget()` extensions and exhaustive `when`s
replace them. Adding a node has **zero** registration points — every remaining
dispatch branch is one the compiler demands. The wire format is byte-identical:
the whole serialization suite carried over **unedited**.

**✅ Working — modifiers from the wire (Phase 3).** Styling/layout was merged
to `master` via #9 (`f542d02`): an ordered `modifiers` list, a small curated
allow-list (padding, size, fill, background, weight), and the column
arrangement/alignment vocabulary. The shared modifier models, serialization,
and the full `commonTest` round-trip/validation suite are in and green, and the
Android fold/scope/renderer changes are in (the two interim `fillMaxWidth()`
hardcodes and `TextWidget`'s dead `modifier` field are gone).

**✅ Working — the Phase 4 catalog.** `row` (column's horizontal counterpart,
with `RowRenderScope` so `weight` works horizontally), `box` (overlay container
with a two-dimensional `contentAlignment`; provides no weight scope), and
`image` (a URL loaded by Coil 3, with `contentDescription` and `contentScale`),
plus a `spacing` field (`Arrangement.spacedBy`, dp) on both `row` and `column`,
mutually exclusive with the arrangement token. Each landed as its own files
plus compiler-demanded `when` branches — the proof the sealed shape holds.
`button` is deliberately **not** here: a button that cannot act is the same
"payload that lies" that moved `clickable` to Phase 5, so it lands there.

**⚠️ Verified on a plain JVM, not yet through the Android build.** The engine
tests (`commonTest` plus the `androidApp` unit tests — 94 engine cases, 64 + 30) run green on
a scratch JVM harness that compiles `shared` and `androidApp`'s `core/**`
against Compose Multiplatform desktop artifacts — the remote environment used
for this work has no Android SDK and cannot reach Google Maven. That proves the
Kotlin, the serialization, and the widget round-trips; it cannot exercise AGP,
the manifest, lint, or pixels. `:shared:testDebugUnitTest`,
`:androidApp:testDebugUnitTest`, `assembleDebug`, `lint`, `:shared:allTests`,
and the on-device check of the catalog payload still need a local run.

## Known design tensions

Found in review of the Phase 0 code; each was resolved by a specific design
decision and fixed in the Phase 1–2 implementation. Kept here because they
explain *why* the engine changed what it changed.

1. **Two competing construction paths.** `Model.toWidget()` and the factory
   registry both build widgets — and they're entangled: `ColumnWidgetFactory`
   calls `children.map { it.toWidget() }`, so a custom factory registered for
   `TextModel` is silently bypassed for texts inside a column.
   → *`toWidget()` was removed and factories recursed through the registry.
   **Phase 4 went further:** the registry is gone and `toWidget()` came back as
   the single exhaustive mapping — with no registry there is nothing left to
   bypass.*
2. **The demo JSON could never parse.** The sample payload has no type
   discriminators, the serializer registers `polymorphic(Any::class)` instead
   of `polymorphic(KomposerModel::class)`, and `ColumnModel.children` is
   annotated `@Contextual`, which routes it *away* from polymorphism.
   → *Wire format with a required `"type"` field and a corrected module.*
3. **Pixels on the wire.** `SpacerModel.px` bakes device density into a
   server payload; worse, `SpacerModel.toWidget()` ignores it (hardcodes
   `16.dp`) and `SpacerWidget.toModel()` hardcodes `26f`.
   → *dp on the wire (`height`), faithful mapping both ways.*
4. **Widgets carry non-data.** `TextWidget` holds `Modifier`, `TextStyle`, and
   an `onTextLayout` lambda — unserializable by nature, so `toModel()` is
   lossy today. → *Those stay widget-only by design; `toModel()` becomes
   faithful for the specified attribute set — exact for canonical payloads,
   with client-side defaults normalizing to absent.*
5. **Dispatch is duplicated.** Type-`when`s live in `KomposerRenderer`,
   *again* in `RenderColumn`, and again in `GraphBuilder` — three switches to
   update per new widget, and they can drift apart.
   → *`RenderColumn` recurses through `KomposerRenderer`; one render dispatch
   remains. **Phase 4 finished the job:** `GraphBuilder` is deleted and every
   remaining `when` is exhaustive over a sealed type, so the switches can no
   longer drift apart silently — a missing branch is a compile error.*
6. **JVM-only reflection in the core.** Registries and the serializer API are
   keyed on `java.lang.Class`, which cannot compile in `commonMain`.
   → *`KClass` everywhere.*
7. **A `@Composable` visitor.** `Accept`/`Visit` are composable but perform no
   composition, forcing pure traversal into the composition (and re-logging
   the debug graph every recomposition). → *De-composed in Phase 2; the
   visitor itself is deleted in Phase 4, replaced by `debugGraph()`.*

---

## Where things are going

- **[ROADMAP.md](ROADMAP.md)** — direction and milestones, deliberately coarse.
  Phases 0–4 are done: the shared KMP contract (models + real JSON round-trip
  in `commonMain`), the Android pipeline that renders raw JSON on screen, the
  ordered modifier system, and Phase 4's sealed architecture plus the catalog
  it was built for (`row`, `box`, `image`, `spacing`). Phase 5 (actions, state,
  `clickable`, `button`) is next.

## Project layout

The two halves of the engine are two Gradle modules, and that split *is* the
architecture: `shared` must stay portable, `androidApp` owns everything
Compose-typed.

```
Komposer/
├── androidApp/                       # Android host + the Compose-aware half of the engine
│   └── src/main/java/ir/gity/komposer/
│       ├── android/MainActivity.kt   # End-to-end wiring + demos (JSON demo is primary)
│       └── core/
│           ├── widget/               # sealed KomposerWidget + Text/Column/Row/Box/Image/Spacer
│           │                         #   widgets, toWidget() mapping, color parsing, debugGraph()
│           ├── renderer/             # KomposerRenderer + Render* + modifier fold + render scopes
│           │                         #   (Column/Row); RenderImage is Coil's AsyncImage
│           └── KomposerRenderException.kt
├── shared/                           # KMP module — the wire contract
│   └── src/commonMain/kotlin/ir/gity/komposer/core/
│       ├── model/                    # sealed KomposerModel + Text/Column/Row/Box/Image/Spacer,
│       │                             #   KomposerDocument, layout/ (per-axis enums), modifier/
│       └── serialization/            # DefaultKomposerSerializer + exceptions
├── iosApp/                           # iOS host (consumes shared as a static framework)
└── ROADMAP.md
```

Nothing in `shared/src/commonMain` may import `android.*`, `androidx.*`, or
`java.*` — that is what keeps the wire contract portable, and the iOS targets
enforce it at compile time. The cheap check:

```bash
grep -rE 'import (androidx|android|java)\.' shared/src/commonMain/   # must print nothing
```

## Build & run

Kotlin Multiplatform via the Gradle wrapper.

**Toolchain** (source of truth: `gradle/libs.versions.toml`):
Kotlin `2.2.0` · AGP `8.12.0` · Gradle `8.14.3` · Compose BOM `2025.04.00` ·
Coil `3.2.0` · kotlinx.serialization `1.8.0` · `compileSdk 35` / `minSdk 24` ·
JVM target `1.8`.

```bash
./gradlew :androidApp:assembleDebug    # build the Android app
./gradlew :androidApp:installDebug     # install on a device/emulator
./gradlew :shared:testDebugUnitTest    # shared-module unit tests (everyday loop)
./gradlew :shared:allTests             # shared tests across all KMP targets
./gradlew :androidApp:lint
```

iOS: open `iosApp/iosApp.xcodeproj` in Xcode; the shared module is exported as
a static framework named `shared`.

> The build prepends `https://maven.myket.ir` to the repository lists in
> `settings.gradle.kts` — leave it; some resolution relies on it. Configuration
> and build caches are enabled, so if build logic changes behave stale, use
> `./gradlew --no-configuration-cache <task>` or `./gradlew clean`.

## Conventions

- **Packages:** `ir.gity.komposer` for shared/common code,
  `ir.gity.komposer.android` for Android entry points.
- **Branches:** `master` (default); `develop` exists but currently trails
  `master`.
- **Comments:** some inline comments are in **Persian (Farsi)** — preserve them
  when editing surrounding code unless asked otherwise.
- **Wire format changes:** the wire format and public engine API are the
  contract — change them deliberately, and keep the round-trip tests green.
