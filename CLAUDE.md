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

## Ground truth: specs/ and the roadmap

`specs/` holds exact, implementation-ready specifications and is the authority on the wire format and engine behavior. **Specs before code:** any change to the wire format or public engine API goes through a spec in `specs/` first, matching the existing format exactly (see `specs/README.md` for the lifecycle, authoring conventions, and the index table — update the index for any new spec). New specs open at **Proposed**, become **Accepted** after review, and **Implemented** only when their acceptance criteria actually pass — never self-mark Implemented.

Current statuses: **SPEC-0001–0004 are Implemented** (Phases 1–2: wire format, node catalog, shared model/serialization, Android rendering pipeline). **SPEC-0005 (modifier system, Phase 3) is Accepted, not yet implemented** — it is the next implementation target; follow its migration notes (shared first, then androidApp).

`ROADMAP.md` is deliberately coarse and current: Phases 0–2 done; Phase 3 = modifiers (SPEC-0005); Phase 4 = widget catalog + collapse registration friction; Phase 5 = interactivity/state (`clickable` lands there, token reserved); Phase 6 = `jvm()` target + backend DSL + graceful unknown-node fallback. iOS rendering, theming, and a generated JSON Schema are explicitly open, no phase.

## Architecture

Komposer drives Jetpack Compose UI from a serializable **Model** tree:

```
JSON ──parse──► KomposerModel ──Factory──► KomposerWidget ──KomposerRenderer──► @Composable
(wire,               ▲                          │
 versioned)          └──── toModel() ◄──────────┘
```

The engine is split across two modules — this split is the architecture:

### `shared/src/commonMain/kotlin/ir/gity/komposer/core/` — the wire contract (pure KMP)

- `model/KomposerModel.kt` — non-sealed interface; `accept(KomposerModelVisitor)`. Concrete nodes: `model/text/TextModel.kt` (full v1 attribute set + `@Serializable` wire-enum classes), `model/column/ColumnModel.kt`, `model/spacer/SpacerModel.kt`. All `@Serializable` with `@SerialName` wire tokens; validation is `require` in `init` (never `check`).
- `model/KomposerDocument.kt` — the envelope: required `version` (must be `1`) + `root`.
- `model/KomposerModelVisitor.kt` — dependency-free visitor over the model tree (usable server-side).
- `serialization/KomposerSchema.kt` — the single wire-level registration point: `polymorphic(KomposerModel::class)` (never `Any::class`).
- `serialization/DefaultKomposerSerializer.kt` — the real, working serializer: `classDiscriminator = "type"`, `ignoreUnknownKeys = true`, `encodeDefaults = false`; nodes go through `PolymorphicSerializer`; all failures wrap into `KomposerParseException`.

**KMP purity rule (non-negotiable):** nothing in `shared/src/commonMain` may import `android.*`, `androidx.*`, or `java.*` (use `KClass`, not `Class`). Guard: `grep -rE 'import (androidx|android|java)\.' shared/src/commonMain/` must return nothing.

### `androidApp/src/main/java/ir/gity/komposer/core/` — the Compose-aware half

- `KomposerWidget.kt` — the "Element" in a GoF Visitor; `toModel()` + non-composable `Accept(visitor)`. Concrete widgets under `widget/{text,column,spacer}/`, each with a `Render*` composable. `ColumnWidget` implements `KomposerCompositeWidget` (`addChild`/`removeChild`/`getChildren`).
- `widget/factory/` — `KomposerWidgetFactory` takes a `root` factory parameter so composites recurse *through the registry* (never construct children directly — that reintroduces the nested-child factory-bypass bug). `FactoryRegistry` is `KClass`-keyed; `build()` snapshots into `DefaultKomposerWidgetFactory`. `KomposerColor.kt`'s `parseKomposerColor` parses hex directly — don't switch it to `android.graphics.Color.parseColor` (drags Robolectric into plain JVM tests).
- `renderer/KomposerRenderer.kt` — the single render dispatch: a `when` over widget types with else-throws (`KomposerRenderException`). Deliberately still a `when` until Phase 4.
- `visitor/KomposerWidgetVisitor.kt` + `GraphBuilder` — debug traversal; not `@Composable` by design.
- `base/NiceToHave.kt` — scratchpad of parked sketches (`KomposerState` for Phase 5, the `Specification` seed). Design sketches, not stable API; pieces graduate out as specs make them real.

End-to-end wiring is in `androidApp/.../android/MainActivity.kt`: `KomposerJson2ModelDemo()` (primary demo — raw JSON string to pixels, this works) and `KomposerModelDemo()` (in-memory path, kept as `@Preview`).

### Key invariants (from the implemented specs)

- **Wire conventions (SPEC-0001):** envelope `{"version": 1, "root": …}`; every node carries a required `"type"` discriminator; dimensions are dp/sp numbers; colors are `#RRGGBB`/`#AARRGGBB` strings; enum tokens are lowerCamelCase. Strictness: unknown node `type` **fails** (`KomposerParseException`); unknown *fields* on known nodes are ignored. v1 stays strict — graceful unknown-node fallback is deliberately Phase 6.
- **Optional wire field ⇒ nullable Kotlin property defaulting to `null`.** The FACTORY (mapping layer) applies Compose defaults — never the model.
- **Round-trip (SPEC-0004 §4):** `create(model).toModel() == model` for canonical models; widget-side values equal to Compose defaults normalize back to absent (`null`); normalization is idempotent. `JSON → Model → Widget → Model → JSON` is lossless for canonical v1 payloads — every change must keep it that way (`ToModelRoundTripTest`, `RoundTripTest`).
- Vocabulary: "wire" = JSON, "model" = the `@Serializable` Kotlin class, "widget" = the Compose-aware object.

### Adding a new widget type

New files: the model (`shared/.../core/model/<node>/`), the widget + `Render<Node>.kt` (`androidApp/.../core/widget/<node>/`), and a factory. Then **five** registration/dispatch points to touch:

1. `KomposerSchema` — `subclass(<Node>Model::class)`
2. Factory registration at the composition root (see `v1Registry()` in `MainActivity.kt`)
3. `KomposerRenderer` — a branch in the `when`
4. `KomposerWidgetVisitor` — a `Visit` overload, plus `GraphBuilder`'s dispatch `when`
5. `KomposerModelVisitor` — a `visit` overload

Collapsing these five into one registration is roadmap Phase 4 — if a task is "make adding widgets less invasive," that's the friction to attack. Per the spec-before-code rule, a new node also needs a node-catalog spec entry (follow SPEC-0002's format).

### Interim hardcodes (dissolved by SPEC-0005 — don't build on them)

`RenderColumn` and `RenderSpacer` currently hardcode `Modifier.fillMaxWidth()` (flagged in SPEC-0002 as interim), and `TextWidget.modifier: Modifier` is a wire-invisible dead path. SPEC-0005 §5.5 removes all three when Phase 3 is implemented.

## Repo conventions

- Package root: `ir.gity.komposer` (shared/common code), `ir.gity.komposer.android` (Android app entry points). Namespaces are set in each module's `build.gradle.kts`.
- Some inline comments are written in Persian (Farsi). Preserve them when editing surrounding code unless asked otherwise.
- Branches on the `Gity-Market/Komposer` remote: `master` (default) and `develop` (currently trailing `master`).
- Root docs (`README.md`, `ROADMAP.md`, this file) must be reconciled in the same PR that lands a phase — stale root docs mislead every future session; `specs/` + git history are ground truth when they disagree.
