# Komposer

> **Server-Driven UI for Jetpack Compose — written Kotlin-first, for Kotlin everywhere.**

Komposer is an experiment in driving native UI from the server. A backend sends a
serializable description of a screen; the client deserializes it into a **Model**
tree, turns that into a tree of **Widgets**, and renders those widgets as real
Jetpack Compose `@Composable`s. The same Model can be turned back into JSON, so the
pipeline is symmetric.

The name is deliberate: this is the **Compose** successor to our production
**XML-based** server-driven UI. The goal is to keep the proven idea — ship UI as data —
while moving the client to a modern, declarative toolkit and (eventually) sharing the
model definitions with a **Kotlin backend** via Kotlin Multiplatform.

---

## Why

- **Ship UI without shipping an app.** Layouts, ordering, copy, and styling become
  data the server controls. No store release to change a screen.
- **One source of truth.** If the Model schema is plain Kotlin in a shared module,
  the **same types** describe the UI on the client *and* are produced by the backend.
  No drift between a hand-written JSON contract and the client parser.
- **Compose-native rendering.** The output is ordinary Compose, not a webview or a
  custom canvas — so it composes, recomposes, themes, and previews like any other
  Compose UI.

---

## The pipeline

```
                         ┌──────────── round-trips back to JSON ────────────┐
                         │                                                   │
   JSON  ──deserialize──►  KomposerModel  ──Factory──►  KomposerWidget  ──KomposerRenderer──►  @Composable
   (wire)                  (serializable)   (build)      (Compose-aware)      (when dispatch)      (UI)
                                  ▲                            │
                                  └──────── toModel() ◄────────┘
```

- **`KomposerModel`** — the serializable shape of a node. Pure data, knows how to
  become a widget (`toWidget()`) and accepts a model visitor.
- **`KomposerWidget`** — the in-memory, Compose-aware node. Knows how to become a
  model again (`toModel()`) and accepts a widget visitor (`Accept`).
- **`KomposerWidgetFactory`** — one factory per Model type; builds the matching
  Widget. Lets construction carry platform context (e.g. `Density` for the spacer).
- **`KomposerRenderer`** — the single dispatch point from a Widget tree into Compose.

Three concrete node types exist today: **Text**, **Column**, and **Spacer**.

---

## Design patterns in play

Komposer is intentionally a study in classic patterns; the code mirrors the GoF
vocabulary so the structure is easy to talk about:

| Pattern | Where | Role |
| --- | --- | --- |
| **Composite** | `ColumnWidget` / `KomposerCompositeWidget` | A node that holds children (`addChild` / `getChildren`). |
| **Factory** | `KomposerWidgetFactory` + `FactoryRegistry` | One factory per Model class, dispatched by a `Map<Class<…>, Factory>`. |
| **Visitor** | `KomposerWidgetVisitor` / `GraphBuilder` | Traversal decoupled from nodes — today it dumps a debug graph of the tree. |
| **Specification** | `NonEmptyTextSpecification` | Validation rules expressed as composable predicates. |

---

## What works today vs. what's stubbed

This is early, exploratory code. Be honest about the seams:

✅ **Working — the in-memory path.**
`KomposerModelDemo()` in `MainActivity.kt` builds a `ColumnModel` by hand, runs it
through the `FactoryRegistry` to get a `KomposerWidget` tree, walks it with
`GraphBuilder` for a debug dump, and renders it with `KomposerRenderer`. This renders
on screen.

🚧 **Not working yet — the JSON path.**
`DefaultKomposerSerializer.serialize` / `deserialize` are `TODO()`. So
`KomposerJson2ModelDemo()` — the *actual* server-driven entry point — does **not**
run end-to-end. The polymorphic `SerializersModule` is sketched but not wired.
`DefaultKomposerMapper` and `widgetToModel` are also stubs.

🧭 **Not multiplatform yet.**
Despite being a KMP project, **all engine code currently lives in
`androidApp/src/main/java/ir/gity/komposer/core/`**. The `shared/` module is still the
KMP skeleton (`Greeting`, `Platform` expect/actual). Making Komposer multiplatform is
a real, planned move — see the roadmap.

`core/base/NiceToHave.kt` is a deliberate scratchpad holding half-finished
abstractions (`KomposerEngine`, `KomposerState`, the JSON factory, the mappers). Treat
it as a design sketch, not stable API — pieces graduate out of it as they become real.

---

## Project layout

```
Komposer/
├── androidApp/                       # Android host + (currently) the whole engine
│   └── src/main/java/ir/gity/komposer/
│       ├── android/MainActivity.kt   # End-to-end wiring + demos
│       └── core/
│           ├── KomposerWidget.kt           # "Element" interface
│           ├── model/                      # Serializable Models (text, column, spacer)
│           ├── widget/                     # Widgets + per-widget Render* + factories
│           ├── renderer/KomposerRenderer.kt# Widget → Compose dispatch (a `when`)
│           ├── visitor/                    # Visitor + GraphBuilder (debug traversal)
│           └── base/NiceToHave.kt          # Scratchpad of future abstractions
├── shared/                           # KMP skeleton (engine will move here)
└── iosApp/                           # iOS host (Xcode project; shared as static framework)
```

---

## Adding a new widget today

Adding, say, a `Row` currently means touching **seven** places — Model, Widget +
`Render*`, Factory, the renderer `when`, the visitor (`Visit` + dispatch), the
serializer's polymorphic module, and the `FactoryRegistry` registration. The registry
comment claims "only one place to add," but the renderer/visitor/serializer `when`s
make that aspirational. **Collapsing this to one registration point is an explicit
roadmap goal** (Phase 4).

---

## Build & run

Kotlin Multiplatform via the Gradle wrapper.

**Toolchain** (source of truth is `gradle/libs.versions.toml`):
Kotlin `2.2.0` · AGP `8.12.0` · Gradle `8.14.3` · Compose BOM `2025.04.00` ·
kotlinx.serialization `1.8.0` · `compileSdk 35` / `minSdk 24` · JVM target `1.8`.

```bash
./gradlew :androidApp:assembleDebug    # build the Android app
./gradlew :androidApp:installDebug     # install on a device/emulator
./gradlew :shared:allTests             # run shared tests across KMP targets
./gradlew :androidApp:lint
```

iOS: open `iosApp/iosApp.xcodeproj` in Xcode. The shared module is exported as a
static framework named `shared`.

> The build prepends `https://maven.myket.ir` to the repository list in
> `settings.gradle.kts` — leave it; some resolution relies on it. Configuration and
> build caches are on, so invalidate with `./gradlew clean` if you see stale build
> behavior after changing build logic.

---

## Roadmap

The plan is deliberately **coarse** — direction and milestones, not tasks — because
there's real ambiguity ahead (especially translating the full space of Compose
modifiers to and from JSON). In short:

| Phase | Goal |
| --- | --- |
| **0 — Foundation** *(largely done)* | Core abstractions, minimal node set, in-memory `Model → Widget → Renderer` path. |
| **1 — Close the JSON loop** | Real polymorphic JSON ⇄ Model so a server payload renders end-to-end. |
| **2 — Go multiplatform** | Lift the engine into `shared/commonMain`; unlock a shared Kotlin backend. |
| **3 — The modifier problem** | A serializable, ordered subset of Compose `Modifier`, grown deliberately. |
| **4 — Widget catalog & lower friction** | More nodes, and one place to register a new widget. |
| **5 — Interactivity & state** | Server-described actions/events and real save/restore. |
| **6 — Backend & tooling** | Kotlin DSL for producing models, schema versioning, validation. |

See **[ROADMAP.md](ROADMAP.md)** for each phase's goal and "done when" signal.

---

## Conventions

- **Packages:** `ir.gity.komposer` for shared/common code; `ir.gity.komposer.android`
  for Android entry points.
- **Branches:** `master` (default), `develop`, `playground`. Use **`playground`** for
  experimental work.
- **Language in comments:** some inline comments are in **Persian (Farsi)** — preserve
  them when editing surrounding code unless asked otherwise.
