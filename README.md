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
- **A pattern study, on purpose.** The codebase deliberately exercises the
  classic GoF vocabulary (Composite, Factory, Visitor) so the architecture is
  easy to name, discuss, and critique.

## Scoping principle

Mapping the *entire* Compose API surface (every modifier, every parameter)
onto JSON is the known hard problem, and it is deliberately deferred. Instead,
one node — **Text** — gets a genuinely rich attribute set (`maxLines`,
`fontWeight`, `color`, `overflow`, …) to prove the wire format against a real
composable, while everything else stays minimal until the
[modifier problem](ROADMAP.md#phase-3--the-modifier-problem-the-hard-one) is
tackled on its own terms — designed in
[SPEC-0005](specs/0005-modifier-system.md) and now implemented (Phase 3).

---

## The pipeline

```
                    ┌────────────── encode() back to JSON ──────────────┐
                    │                                                    │
 JSON ───parse────► KomposerModel ───Factory───► KomposerWidget ───KomposerRenderer───► @Composable
 (wire,             (@Serializable,   (registry,   (Compose-aware        (when
  versioned)         pure Kotlin)      recursive)   value object)         dispatch)
                          ▲                              │
                          └───────── toModel() ◄─────────┘
```

- **`KomposerModel`** — the serializable shape of a node. Pure data; the KMP
  citizen that the backend will share.
- **`KomposerWidget`** — the in-memory, Compose-aware node. Knows how to turn
  back into a model (`toModel()`) and accepts a visitor (`Accept`).
- **`KomposerWidgetFactory`** + **`FactoryRegistry`** — one factory per Model
  type; the *only* Model → Widget path. Registered per model class, dispatched
  by map lookup.
- **`KomposerRenderer`** — the single dispatch point from a Widget tree into
  Compose.

Three node types exist today: **Text**, **Column** (the composite), **Spacer**.

## Design patterns in play

| Pattern | Where | Role |
| --- | --- | --- |
| **Composite** | `ColumnWidget` : `KomposerCompositeWidget` | A node that holds children (`addChild` / `getChildren`). |
| **Factory** | `KomposerWidgetFactory` + `FactoryRegistry` | One factory per Model class; carries platform context construction needs. |
| **Visitor** | `KomposerWidgetVisitor` / `GraphBuilder` | Traversal decoupled from node classes; today it dumps a debug graph. |
| **Specification** | `NonEmptyTextSpecification` | Seed of predicate-style validation (no combinators yet — a sketch, not a framework). |

---

## Status: what works, what's stubbed

This is early, exploratory code, and honesty about the seams is a feature.

**✅ Working — the JSON path (Phases 1–2, SPEC-0001–0004).**
`KomposerJson2ModelDemo()` in `MainActivity.kt` is the primary demo: a raw JSON
document (the [SPEC-0001 §7](specs/0001-json-wire-format.md) reference payload)
is parsed by `DefaultKomposerSerializer`, built into a widget tree through the
`FactoryRegistry`, and rendered as styled pixels.
`JSON → Model → Widget → Model → JSON` is lossless for canonical v1 payloads,
tested in `commonTest` and `androidApp` unit tests.

**✅ Working — the in-memory path.** `KomposerModelDemo()` builds a
`ColumnModel` by hand, runs it through the same registry, walks it with
`GraphBuilder` for a debug dump, and renders it. Kept as an `@Preview`.

**✅ Multiplatform where it counts.** The model + serialization layer lives in
`shared/src/commonMain` — pure Kotlin, no Compose/Android/`java.*` imports —
and compiles for Android and the three iOS targets. Compose-typed code
(widgets, factories, renderer, visitor) stays in `androidApp` by design until
the rendering story goes multiplatform. One caveat: a Kotlin *backend* sharing
the types still needs a `jvm()` target on `shared`, a one-line build change
deliberately deferred to
[roadmap Phase 6](ROADMAP.md#phase-6--backend--tooling).

**✅ Working — modifiers from the wire (Phase 3, SPEC-0005).** Styling/layout
is specified in [SPEC-0005](specs/0005-modifier-system.md) and merged to
`master` via #9 (`f542d02`): an ordered `modifiers` list, a small curated
allow-list (padding, size, fill, background, weight), and the column
arrangement/alignment vocabulary. The shared modifier models, serialization,
and the full `commonTest` round-trip/validation suite are in and green, and the
Android fold/scope/renderer changes are in (the two interim `fillMaxWidth()`
hardcodes and `TextWidget`'s dead `modifier` field are gone, §5.5). One
deferred check: the §10 device/`assembleDebug`/`lint` acceptance run still
needs an environment with Google-Maven (AGP + Compose) access.

`core/base/NiceToHave.kt` is a deliberate scratchpad of the remaining
half-finished sketches (`KomposerState` for Phase 5, the `Specification` seed).
The serializer, mappers, engine, JSON factory, model visitor, and factory
registry have all graduated out of it (or been deleted) as specs made them
real.

## Known design tensions

Found in review of the Phase 0 code; each was resolved by a specific spec
decision and fixed in the Phase 1–2 implementation. Kept here because they
explain *why* the specs changed what they changed.

1. **Two competing construction paths.** `Model.toWidget()` and the factory
   registry both build widgets — and they're entangled: `ColumnWidgetFactory`
   calls `children.map { it.toWidget() }`, so a custom factory registered for
   `TextModel` is silently bypassed for texts inside a column.
   → *`toWidget()` is removed; factories recurse through the registry
   ([SPEC-0003 §2](specs/0003-model-layer-and-serialization.md),
   [SPEC-0004 §2](specs/0004-android-rendering-pipeline.md)).*
2. **The demo JSON could never parse.** The sample payload has no type
   discriminators, the serializer registers `polymorphic(Any::class)` instead
   of `polymorphic(KomposerModel::class)`, and `ColumnModel.children` is
   annotated `@Contextual`, which routes it *away* from polymorphism.
   → *Wire format with a required `"type"` field and a corrected module
   ([SPEC-0001](specs/0001-json-wire-format.md), SPEC-0003 §3).*
3. **Pixels on the wire.** `SpacerModel.px` bakes device density into a
   server payload; worse, `SpacerModel.toWidget()` ignores it (hardcodes
   `16.dp`) and `SpacerWidget.toModel()` hardcodes `26f`.
   → *dp on the wire (`height`), faithful mapping both ways
   ([SPEC-0002 §3](specs/0002-node-catalog-v1.md)).*
4. **Widgets carry non-data.** `TextWidget` holds `Modifier`, `TextStyle`, and
   an `onTextLayout` lambda — unserializable by nature, so `toModel()` is
   lossy today. → *Those stay widget-only by design; `toModel()` becomes
   faithful for the specified attribute set — exact for canonical payloads,
   with client-side defaults normalizing to absent (SPEC-0002, SPEC-0004 §4).*
5. **Dispatch is duplicated.** Type-`when`s live in `KomposerRenderer`,
   *again* in `RenderColumn`, and again in `GraphBuilder` — three switches to
   update per new widget, and they can drift apart.
   → *`RenderColumn` recurses through `KomposerRenderer`; one render dispatch
   remains (SPEC-0004 §5). Collapsing the rest is roadmap Phase 4.*
6. **JVM-only reflection in the core.** Registries and the serializer API are
   keyed on `java.lang.Class`, which cannot compile in `commonMain`.
   → *`KClass` everywhere (SPEC-0003 §5, SPEC-0004 §1).*
7. **A `@Composable` visitor.** `Accept`/`Visit` are composable but perform no
   composition, forcing pure traversal into the composition (and re-logging
   the debug graph every recomposition). → *De-composed in SPEC-0004 §5.*

---

## Where things are going

- **[ROADMAP.md](ROADMAP.md)** — direction and milestones, deliberately coarse.
  Phases 0–3 are done: the shared KMP contract (models + real JSON round-trip
  in `commonMain`), the Android pipeline that renders raw JSON on screen, and
  the ordered modifier system. Next up: Phase 4 — widget catalog & lower
  registration friction.
- **[specs/](specs/)** — exact, implementation-ready specs.
  [0001](specs/0001-json-wire-format.md)–[0005](specs/0005-modifier-system.md)
  are **Implemented** and now serve as documentation of the wire format, node
  catalog, serialization engine, rendering pipeline, and modifier system (one
  deferred check: SPEC-0005's device/`assembleDebug`/`lint` acceptance run
  awaits a Google-Maven-capable environment). Field names, defaults, error
  behavior, acceptance criteria — implementation is mostly transcription.

## Project layout

```
Komposer/
├── androidApp/                       # Android host + the Compose-aware half of the engine
│   └── src/main/java/ir/gity/komposer/
│       ├── android/MainActivity.kt   # End-to-end wiring + demos (JSON demo is primary)
│       └── core/
│           ├── KomposerWidget.kt            # "Element" interface
│           ├── widget/                      # Widgets + per-widget Render* + factories/registry
│           ├── renderer/KomposerRenderer.kt # Widget → Compose dispatch
│           ├── visitor/                     # Visitor + GraphBuilder (debug traversal)
│           └── base/NiceToHave.kt           # Scratchpad of future abstractions
├── shared/                           # KMP module — the wire contract
│   └── src/commonMain/kotlin/ir/gity/komposer/core/
│       ├── model/                    # @Serializable Models (text/column/spacer) + model visitor
│       └── serialization/            # KomposerSchema + DefaultKomposerSerializer
├── iosApp/                           # iOS host (consumes shared as a static framework)
├── specs/                            # Exact specs — 0001–0005 Implemented
└── ROADMAP.md
```

## Build & run

Kotlin Multiplatform via the Gradle wrapper.

**Toolchain** (source of truth: `gradle/libs.versions.toml`):
Kotlin `2.2.0` · AGP `8.12.0` · Gradle `8.14.3` · Compose BOM `2025.04.00` ·
kotlinx.serialization `1.8.0` · `compileSdk 35` / `minSdk 24` · JVM target `1.8`.

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
- **Specs before code:** changes to the wire format or public engine API go
  through a spec in [`specs/`](specs/) first.
