# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & run

Kotlin Multiplatform project, Gradle wrapper `8.14.3`, Kotlin `2.2.0` / AGP `8.12.0`, `compileSdk = 35`, `minSdk = 24`, JVM target `1.8`. The version catalog lives at `gradle/libs.versions.toml` — add/update dependencies there, not in module `build.gradle.kts` files.

Common tasks (use the wrapper):

- `./gradlew :shared:testDebugUnitTest` — shared-module unit tests (the everyday loop)
- `./gradlew :shared:allTests` — shared tests across all KMP targets (iOS test *execution* needs macOS/Xcode)
- `./gradlew :androidApp:testDebugUnitTest` — Android-side unit tests (widget round-trip, color parsing)
- `./gradlew :androidApp:assembleDebug` — build the Android app
- `./gradlew :androidApp:installDebug` — install on a connected device/emulator
- `./gradlew :androidApp:lint`
- iOS app: open `iosApp/iosApp.xcodeproj` in Xcode. The shared module is exported as a static framework named `shared` (see `shared/build.gradle.kts`).

The build adds `https://maven.myket.ir` ahead of Google/Maven Central in both `pluginManagement` and `dependencyResolutionManagement` (settings.gradle.kts) — do not remove it; some plugin/library resolution relies on it. Gradle configuration cache and build cache are both enabled in `gradle.properties`, so if you change build logic and see stale behavior, invalidate with `./gradlew --no-configuration-cache <task>` or `./gradlew clean`.

## Ground truth: the roadmap

`ROADMAP.md` is deliberately coarse and current: Phases 0–4 done; Phase 5 = interactivity/state (`clickable` and the `button` node land there — both deferred because a control that cannot act is a payload that lies); Phase 6 = `jvm()` target + backend DSL + graceful unknown-node fallback. iOS rendering, theming, a generated JSON Schema, and post-Phase-4 catalog growth (lazy lists, per-child `align`, horizontal spacer, image sources beyond URL) are explicitly open, no phase.

Phases 1–3 are implemented: wire format, node catalog, shared model/serialization, the Android rendering pipeline, and the modifier system (merged to `master` via #9, `f542d02`). The Phase 4 architecture simplification landed 2026-09-03: `KomposerModel` and `KomposerWidget` are sealed; the schema object, both visitors, the factory layer, and the composite interface are deleted; `toWidget()` extension functions and exhaustive `when` dispatch replace them. The Phase 4 catalog landed 2026-09-04 on that shape: `row` (+ `RowRenderScope`), `box`, `image` (Coil 3), and `spacing` on `row`/`column`. The architecture sections below describe the code as it now is.

**Verified on a plain JVM only (say so, don't assume otherwise).** The remote environment used for Phase 4 has no Android SDK and its proxy denies `dl.google.com` and `maven.myket.ir`, so no repo Gradle task can run there. The engine tests (all of `commonTest` plus the `androidApp` unit tests) were instead run green on a scratch JVM harness: a Gradle project compiling `shared/src/commonMain` + `commonTest` and `androidApp/src/{main,test}/java/**/core/**` against Compose Multiplatform desktop `1.8.2` and Coil `3.2.0` from Maven Central (same `androidx.compose.*` packages; `MainActivity`/theme excluded because they import `android.*`). That covers the Kotlin, the serialization, and the widget round-trips; it cannot cover AGP, the manifest, lint, or pixels. `:shared:testDebugUnitTest`, `:androidApp:testDebugUnitTest`, `:shared:allTests`, `assembleDebug`, `lint`, and the on-device visual check (Phase 3's and Phase 4's catalog payload) have **not** been run against this tree. If you have a working build, running them and reconciling these docs is a high-value first task.

There is no `specs/` directory. It existed through Phase 3 and was removed when the architecture simplification landed — the code plus git history is the authority on the wire format now. Do not re-cite `SPEC-000X §Y`; those documents are only in history.

## Architecture

Komposer drives Jetpack Compose UI from a serializable **Model** tree:

```
JSON ──parse──► KomposerModel ──toWidget()──► KomposerWidget ──KomposerRenderer──► @Composable
(wire,               ▲     (sealed)      │        (sealed)        (exhaustive when)
 versioned)          └──── toModel() ◄───┘
```

The engine is split across two modules — this split is the architecture. Keep the halves separable: `shared` must stay portable, and the iOS targets enforce that at compile time.

### `shared/src/commonMain/kotlin/ir/gity/komposer/core/` — the wire contract (pure KMP)

- `model/KomposerModel.kt` — **`@Serializable sealed interface`** carrying only `val modifiers: List<KomposerModifier>`. Sealing *is* the registration: the annotation makes the plugin emit the closed `SealedClassSerializer`, so there is no module to register and no schema object. Kotlin's same-package rule for sealed implementors is why the concrete nodes sit **beside** it in `core/model/`: `TextModel.kt` (full v1 attribute set + `@Serializable` wire-enum classes), `ColumnModel.kt` and `RowModel.kt` (each with its per-axis arrangement/alignment pair plus `spacing`), `BoxModel.kt` (`contentAlignment`, the two-dimensional vocabulary), `ImageModel.kt` (`url` required and non-blank, `contentDescription`, `contentScale` + its `ContentScaleValue` enum), `SpacerModel.kt`. All `@Serializable` with `@SerialName` wire tokens; validation is `require` in `init` (never `check`). `model/layout/LayoutValues.kt` holds the five container enums — one class per axis, deliberately (`VerticalArrangementValue`/`HorizontalAlignmentValue` for column, `HorizontalArrangementValue`/`VerticalAlignmentValue` for row, `AlignmentValue` for box); never reuse one across axes.
- `model/modifier/` — the ordered modifier system: `KomposerModifier` is a **`@Serializable` sealed interface** — the precedent `KomposerModel` now follows. Seven `@SerialName` data classes: `PaddingModifier`, `SizeModifier`, `FillMax{Width,Height,Size}Modifier`, `BackgroundModifier`, `WeightModifier`. `model/WireColor.kt` holds the shared color regex (promoted out of `TextModel`).
- `model/KomposerDocument.kt` — the envelope: required `version` (must be `1`) + `root`.
- `serialization/DefaultKomposerSerializer.kt` — the real, working serializer: `classDiscriminator = "type"`, `ignoreUnknownKeys = true`, `encodeDefaults = false`; **no `serializersModule`** — nodes go through `serializer<KomposerModel>()`, the plugin-generated sealed serializer. All failures wrap into `KomposerParseException`. Do not encode a node via its concrete-class serializer: that writes no `type` discriminator and produces JSON that can never be parsed back.
- There is deliberately **no** model visitor and **no** schema object. When traversal is needed it is a plain function with an exhaustive `when` — the compiler gives the "you forgot a node type" guarantee the visitor used to.

**KMP purity rule (non-negotiable):** nothing in `shared/src/commonMain` may import `android.*`, `androidx.*`, or `java.*` (use `KClass`, not `Class`). Guard: `grep -rE 'import (androidx|android|java)\.' shared/src/commonMain/` must return nothing.

### `androidApp/src/main/java/ir/gity/komposer/core/` — the Compose-aware half

- `widget/KomposerWidget.kt` — **sealed interface** (not `@Serializable`; widgets never touch the wire): `val modifiers` + `toModel()`. That is the layer's whole job. `TextWidget`, `ColumnWidget`, `RowWidget`, `BoxWidget`, `ImageWidget`, `SpacerWidget` sit beside it in the same package (sealed same-package rule). The containers are immutable `data class`es holding `children: List<KomposerWidget>` — the mutable composite API is gone. `ColumnWidget`/`RowWidget` store `spacing` as their own `Dp?` (never folded into the `Arrangement`, whose `spacedBy` dp could not be recovered by `toModel()`) and `require` it absent when the arrangement is non-default, mirroring the model rule.
- `widget/KomposerWidgetMapping.kt` — `KomposerModel.toWidget()`, the **single** Model → Widget path: one exhaustive `when` with no `else`. Per-node `toWidget()` extensions live in each widget's own file, so construction and `toModel()` normalization face each other. There is no registry, therefore nothing to bypass — the old nested-child factory-bypass bug class is structurally impossible.
- `widget/KomposerColor.kt` — `parseKomposerColor` parses hex directly; don't switch it to `android.graphics.Color.parseColor` (drags Robolectric into plain JVM tests).
- `widget/DebugGraph.kt` — `KomposerWidget.debugGraph()`, a plain non-composable traversal; replaced the widget visitor + `GraphBuilder`.
- `renderer/KomposerRenderer.kt` — the single render dispatch: an **exhaustive** `when` over the sealed widget types, **no `else`** — a widget without a render branch is a compile error, which is why the old `else -> throw` is gone. Threads a `scope: KomposerRenderScope?` down so `weight` can reach its parent `ColumnScope`/`RowScope`. `Render{Text,Column,Row,Box,Image,Spacer}.kt` sit beside it in `renderer/`. `RenderColumn`/`RenderRow` compose `spacing` as `Arrangement.spacedBy(it)` when present, else the stored arrangement. `RenderBox` hands its children `scope = null` — `BoxScope` has no `weight`. `RenderImage` is Coil's `AsyncImage(model = url, …)`; the OkHttp fetcher self-registers from the classpath, so there is no loader setup anywhere.
- `renderer/KomposerModifierFold.kt` + `renderer/KomposerRenderScope.kt` — `List<KomposerModifier>.toComposeModifier(scope)` folds the ordered list into a real `Modifier` via one **exhaustive** `when` over the sealed hierarchy (no `else` — the compiler forces a branch per modifier). Widgets store the model list verbatim (`toModel()` on modifiers is identity), so the round-trip is exact for every list; `weight` folds through `KomposerRenderScope` (`ColumnRenderScope` / `RowRenderScope`) and throws where no scope exists: at the root, or directly under a `box`.
- `KomposerRenderException.kt` — now has exactly two throw sites, both payload errors: a scope-less `weight` fold and an unparseable wire color. The "unregistered factory" / "unhandled widget" cases it was written for are gone.

End-to-end wiring is in `androidApp/.../android/MainActivity.kt`: `KomposerJson2ModelDemo()` (primary demo — raw JSON string to pixels, this works) and `KomposerModelDemo()` (in-memory path, kept as `@Preview`).

### Key invariants

- **Wire conventions:** envelope `{"version": 1, "root": …}`; every node carries a required `"type"` discriminator; dimensions are dp/sp numbers; colors are `#RRGGBB`/`#AARRGGBB` strings; enum tokens are lowerCamelCase. Strictness: unknown node `type` **fails** (`KomposerParseException`); unknown *fields* on known nodes are ignored. v1 stays strict — graceful unknown-node fallback is deliberately Phase 6.
- **Optional wire field ⇒ nullable Kotlin property defaulting to `null`.** The MAPPING layer (`toWidget()`) applies Compose defaults — never the model.
- **Round-trip:** `model.toWidget().toModel() == model` for canonical models; widget-side values equal to Compose defaults normalize back to absent (`null`); normalization is idempotent. `JSON → Model → Widget → Model → JSON` is lossless for canonical v1 payloads — every change must keep it that way (`ToModelRoundTripTest`, `RoundTripTest`, `CatalogRoundTripTest`, `CatalogReferenceTest`).
- **`spacing` is an arrangement.** On `row` and `column`, `spacing` (dp, finite, ≥ 0; `0` is legal) maps to `Arrangement.spacedBy` and is **mutually exclusive** with the arrangement token — even the default token spelled out — because Compose has one arrangement slot. Cross-axis alignment combines freely with it. `spacing` round-trips exactly (stored as `Dp?`), so `"spacing": 0` is non-canonical but lossless.
- **`box` provides no weight scope.** Its children render with `scope = null`; `weight` directly under a box parses (a model cannot see its parent) and throws `KomposerRenderException` at the fold, like `weight` at the root. `contentAlignment` positions every child; there is no per-child `align` yet.
- **`image` is a URL.** `url` is required and non-blank; sizing comes from modifiers (`size`, `fillMaxWidth`, …) — with none, Coil lays the image out at zero size until loaded, then its intrinsic size. `INTERNET` is declared in the manifest for it; Coil is pinned to `3.2.0` in the version catalog because that release requires exactly Compose `1.8.0`, the BOM pin (bump Coil and the BOM together).
- Vocabulary: "wire" = JSON, "model" = the `@Serializable` Kotlin class, "widget" = the Compose-aware object.

### Adding a new widget type

Three new files: the model (`shared/.../core/model/<Node>Model.kt` — beside the sealed base, not in a subpackage), the widget + its `toWidget()` (`androidApp/.../core/widget/<Node>Widget.kt`), and `androidApp/.../core/renderer/Render<Node>.kt`. `row`, `box`, and `image` are the worked examples — diff the commit that added each. Then **zero** registration points; what remains is dispatch branches the compiler demands:

| Concern | What you write | Forgetting it is… |
| --- | --- | --- |
| Wire schema | the `@Serializable @SerialName` data class implementing sealed `KomposerModel` | impossible to forget — it *is* the node |
| Model → Widget | a `toWidget()` extension + a branch in `KomposerWidgetMapping.kt`'s `when` | a compile error |
| Widget → Compose | the widget implementing sealed `KomposerWidget`, a `Render*` + a branch in `KomposerRenderer`'s `when` | a compile error |
| Round-trip | `toModel()` on the widget + `ToModelRoundTripTest` cases; serialization cases in `CatalogRoundTripTest` / `ValidationTest` / `StrictnessTest` | a test failure |
| Debug dump | a branch in `debugGraph()`'s `when` | a compile error |
| Traversal / visitors / registries | — nothing exists to update — | — |

This replaced the old five-touch-point checklist (registry + schema + two visitors + renderer): Phase 4 dissolved the friction by having nothing to register rather than by building a better registry.

### Interim hardcodes — dissolved in Phase 3

Previously `RenderColumn` and `RenderSpacer` hardcoded `Modifier.fillMaxWidth()` and `TextWidget` carried a wire-invisible `modifier: Modifier` dead path. **All three are now removed:** the renderers fold the wire `modifiers` list instead, and a bare `column` sizes wrap-content like a bare Compose `Column` (add `{"type":"fillMaxWidth"}` for full width). Node-intrinsic modifiers (`spacer`'s `height`) are appended *after* the folded wire modifiers.

## Repo conventions

- Package root: `ir.gity.komposer` (shared/common code), `ir.gity.komposer.android` (Android app entry points). Namespaces are set in each module's `build.gradle.kts`.
- Some inline comments are written in Persian (Farsi). Preserve them when editing surrounding code unless asked otherwise.
- Branches on the `Gity-Market/Komposer` remote: `master` (default) and `develop` (currently trailing `master`).
- Root docs (`README.md`, `ROADMAP.md`, this file) must be reconciled in the same PR that lands a phase — stale root docs mislead every future session; the code + git history are ground truth when they disagree.
