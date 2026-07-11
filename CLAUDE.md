# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & run

Kotlin Multiplatform project, Gradle wrapper `8.14.3`, Kotlin `2.2.0` / AGP `8.12.0`, `compileSdk = 35`, `minSdk = 24`, JVM target `1.8`. The version catalog lives at `gradle/libs.versions.toml` — add/update dependencies there, not in module `build.gradle.kts` files.

Common tasks (use the wrapper):

- `./gradlew :androidApp:assembleDebug` — build the Android app
- `./gradlew :androidApp:installDebug` — install on a connected device/emulator
- `./gradlew :shared:testDebugUnitTest` — run shared-module Android-side unit tests
- `./gradlew :shared:allTests` — run shared tests across all KMP targets
- `./gradlew :shared:iosSimulatorArm64Test` — run iOS-simulator tests (macOS only)
- `./gradlew :androidApp:lint`
- iOS app: open `iosApp/iosApp.xcodeproj` in Xcode. The shared module is exported as a static framework named `shared` (see `shared/build.gradle.kts`).

The build adds `https://maven.myket.ir` ahead of Google/Maven Central in both `pluginManagement` and `dependencyResolutionManagement` (settings.gradle.kts) — do not remove it; some plugin/library resolution relies on it. Gradle configuration cache and build cache are both enabled in `gradle.properties`, so if you change build logic and see stale behavior, invalidate with `./gradlew --no-configuration-cache <task>` or `./gradlew clean`.

## Architecture

Komposer is a small framework that drives Jetpack Compose UI from a serializable **Model** tree. The pipeline is:

```
JSON ──deserialize──► KomposerModel ──Factory──► KomposerWidget ──KomposerRenderer──► @Composable
                          ▲                          │
                          └──── toModel() ◄──────────┘
```

Despite being a KMP project, **all Komposer engine code currently lives in `androidApp/src/main/java/ir/gity/komposer/core/`**, not in `shared/`. The `shared/` module is still the KMP skeleton (`Greeting`, `Platform.kt` expect/actual). If a task involves making Komposer multiplatform, that's a meaningful move — flag it rather than assuming the structure is already in place.

Key types (all under `core/`):

- `KomposerWidget` (`core/KomposerWidget.kt`) — the "Element" in a GoF Visitor; exposes `toModel()` and `@Composable Accept(visitor)`. Concrete widgets: `TextWidget`, `ColumnWidget`, `SpacerWidget`.
- `KomposerModel` (`core/model/KomposerModel.kt`) — serializable counterpart to a widget, with `toWidget()` and `accept(KomposerModelVisitor)`. Concrete models live under `core/model/{text,column,spacer}/`.
- `KomposerCompositeWidget` — GoF Composite; only `ColumnWidget` implements it today (`addChild`/`removeChild`/`getChildren`).
- `KomposerWidgetFactory` + `DefaultKomposerWidgetFactory` (`core/widget/factory/`) — one factory per model class, dispatched by `Map<Class<out KomposerModel>, KomposerWidgetFactory>`. Register factories via `FactoryRegistry` (see `core/base/NiceToHave.kt`).
- `KomposerRenderer(widget)` (`core/renderer/`) — the dispatch point from widget tree to Compose. It is a plain `when` over concrete widget types, **not** the Visitor; adding a new widget type requires editing this `when`.
- `KomposerWidgetVisitor` / `GraphBuilder` (`core/visitor/`) — Visitor for traversal (debug graph dump). `Accept(visitor)` on composite widgets recurses into children.

The end-to-end wiring lives in `androidApp/.../android/MainActivity.kt` — `KomposerModelDemo()` shows the model→factory→widget→renderer flow, and `KomposerJson2ModelDemo()` shows the intended JSON path.

### Things that are stubbed, not done

Be careful before recommending these as working:

- `DefaultKomposerSerializer.serialize` / `deserialize` (`core/base/NiceToHave.kt`) are `TODO("")`. The JSON-driven path (`DefaultKomposerJsonFactory`, `KomposerJson2ModelDemo`) **does not work** yet.
- `DefaultKomposerMapper` and `KomposerWidgetMapper.widgetToModel` are `TODO()`.
- `core/base/NiceToHave.kt` is a scratch file holding several half-finished abstractions (`KomposerEngine`, `Specification`, `KomposerState`, `KomposerModelVisitor`, the JSON factory, two mappers). Treat it as a design sketch, not stable API — split things out as they become real.

### Adding a new widget type

To add e.g. `RowWidget`, you need to touch all of:

1. `core/model/row/RowModel.kt` — `@Serializable data class … : KomposerModel`
2. `core/widget/row/RowWidget.kt` + `RenderRow.kt` — `KomposerWidget` (and `KomposerCompositeWidget` if it has children)
3. `core/widget/factory/RowWidgetFactory.kt` — `KomposerWidgetFactory`
4. `core/renderer/KomposerRenderer.kt` — add a branch to the `when`
5. `core/visitor/KomposerWidgetVisitor.kt` — add `Visit(rowWidget: RowWidget)` and update `GraphBuilder.Visit(widget)` dispatch
6. `core/base/NiceToHave.kt` — add `subclass(RowModel::class)` inside `DefaultKomposerSerializer`'s polymorphic module
7. Register the factory in any `FactoryRegistry` usage (see `MainActivity.kt`)

The factory registry comment in `MainActivity.kt` claims "only one place to add", but in practice the renderer/visitor/serializer `when`s mean it's the seven places above. If a task is "make adding widgets less invasive," that's the friction to attack.

## Repo conventions

- Package root: `ir.gity.komposer` (shared/common code), `ir.gity.komposer.android` (Android app entry points). Namespaces are set in each module's `build.gradle.kts`.
- Some inline comments are written in Persian (Farsi). Preserve them when editing surrounding code unless asked otherwise.
- Branches on the `Gity-Market/Komposer` remote: `master` (default) and `develop` (currently trailing `master`).
