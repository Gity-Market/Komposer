# SPEC-0004 — Android Rendering Pipeline

**Status:** Implemented (2026-07-12)
**Depends on:** SPEC-0003 (shared models & serializer)
**Delivers:** the first true server-driven render — raw JSON string → pixels

## Scope

Rebuild the Android-side pipeline (factories, registry, renderer, demos) on top
of the shared model layer: `KClass`-keyed registration, one recursive
construction path, faithful widget→model mapping for the v1 catalog, and a
`KomposerJson2ModelDemo` that actually runs.

## Non-goals

- Collapsing the renderer/visitor `when`s into the registry (roadmap Phase 4).
  v1 keeps the `when`s but stops *duplicating* them.
- Any new node type or modifier support.

---

## 1. Registry keyed by `KClass`

```kotlin
class FactoryRegistry {
    private val factories =
        mutableMapOf<KClass<out KomposerModel>, KomposerWidgetFactory>()

    fun register(modelClass: KClass<out KomposerModel>, factory: KomposerWidgetFactory) {
        factories[modelClass] = factory
    }

    inline fun <reified T : KomposerModel> register(factory: KomposerWidgetFactory) =
        register(T::class, factory)

    fun build(): DefaultKomposerWidgetFactory = DefaultKomposerWidgetFactory(factories.toMap())
}
```

`java.lang.Class` keys are replaced by `kotlin.reflect.KClass` everywhere
(`FactoryRegistry`, `DefaultKomposerWidgetFactory`); lookup becomes
`factories[model::class]`. `KClass` works identically on Android today and is
the only variant that can follow the registry into `commonMain` later.
`FactoryRegistry` also graduates out of `NiceToHave.kt` into
`core/widget/factory/`. Note `build()` snapshots the registrations
(`factories.toMap()`): a `register()` call after `build()` must not mutate a
factory already handed out.

## 2. One construction path, recursive through the registry

`KomposerWidgetFactory` gains a `root` parameter so composite factories recurse
through the *dispatching* factory instead of the deleted `Model.toWidget()`:

```kotlin
interface KomposerWidgetFactory {
    /** [root] is the top-level dispatching factory; composites use it for children. */
    fun create(model: KomposerModel, root: KomposerWidgetFactory): KomposerWidget
}

class DefaultKomposerWidgetFactory(
    private val factories: Map<KClass<out KomposerModel>, KomposerWidgetFactory>,
) : KomposerWidgetFactory {
    override fun create(model: KomposerModel, root: KomposerWidgetFactory): KomposerWidget {
        val factory = factories[model::class]
            ?: throw KomposerRenderException("No factory registered for ${model::class.simpleName}")
        return factory.create(model, root)
    }
    fun create(model: KomposerModel): KomposerWidget = create(model, this)
}

class ColumnWidgetFactory : KomposerWidgetFactory {
    override fun create(model: KomposerModel, root: KomposerWidgetFactory): KomposerWidget {
        require(model is ColumnModel) { "ColumnWidgetFactory received ${model::class.simpleName}" }
        return ColumnWidget(
            children = model.children.map { root.create(it, root) }.toMutableList(),
        )
    }
}
```

This fixes the silent-bypass bug: a custom factory registered for `TextModel`
now applies to texts nested inside columns too. Leaf factories ignore `root`.
`KomposerRenderException` mirrors `KomposerParseException` for the
construction/render stage. Every type-mismatch `require` carries a message
naming the factory and the class it received, so a mis-registration doesn't
surface as a bare `Failed requirement`.

## 3. Factories become the full mapping layer

**`TextWidgetFactory`** maps *every* SPEC-0002 field, applying Compose defaults
for absent ones:

| Model field (`null` ⇒) | Widget value |
| --- | --- |
| `color` (`Color.Unspecified`) | `parseKomposerColor(str)` — `#RRGGBB`/`#AARRGGBB` → `Color` |
| `fontSize`/`letterSpacing`/`lineHeight` (`TextUnit.Unspecified`) | `value.sp` |
| `fontWeight` (`null`) | `FontWeight(value)` |
| `fontStyle`/`textDecoration`/`textAlign`/`overflow` | exhaustive `when` over the enum |
| `softWrap` (`true`), `maxLines` (`Int.MAX_VALUE`), `minLines` (`1`) | direct |

`parseKomposerColor` lives beside the factory (Compose-typed, so Android layer),
with its own unit test. Implement it by parsing the hex digits into a `Long`
and calling `Color(...)` directly — pure Kotlin against compose-ui, so the test
runs on the JVM; `android.graphics.Color.parseColor` would drag the Android
framework (Robolectric) into what should be a plain unit test.

**`SpacerWidgetFactory`** drops its `Density` constructor parameter — dp on the
wire means `SpacerWidget(model.height.dp)`, no density conversion. (This also
unblocks registering factories outside a composition; today the registry needs
`LocalDensity.current` just to exist.)

**`SpacerWidgetFactory`** moves from `TextWidgetFactory.kt` into its own file.

## 4. Faithful `toModel()` for the v1 catalog

`toModel()` stays on `KomposerWidget` (client-side authoring → JSON is a real
use case for a Kotlin-first SDUI), but it stops lying:

- `TextWidget.toModel()` maps all v1 fields back (Color → hex string via
  `toArgb()`, `FontWeight` → int, enums reversed). `modifier`, `style`,
  `onTextLayout` are excluded by design (SPEC-0002).
- `SpacerWidget.toModel()` returns the real height (`height.value`), replacing
  the hardcoded `26f`.

**Exact equality is impossible for every model, and the spec must say so.**
`TextWidget` stores non-nullable Compose values with real defaults
(`overflow = TextOverflow.Clip`, `softWrap = true`, `maxLines = Int.MAX_VALUE`,
`minLines = 1`), so a model with `"overflow": "clip"` set explicitly and one
with `overflow` absent build *identical* widgets — `toModel()` cannot tell them
apart. Likewise a 6-digit wire color (`"#6200EE"`) comes back from `toArgb()`
as 8-digit. The invariant is therefore defined over **canonical** models:

> A model is **canonical** when every optional field is either absent (`null`)
> or set to a value that differs from SPEC-0002's "Default when absent" column,
> and every color is written as 8-digit uppercase `#AARRGGBB`.

`toModel()` **normalizes** to canonical form:

| Widget value | Model field becomes |
| --- | --- |
| `Color.Unspecified` / `TextUnit.Unspecified` | `null` |
| `overflow == TextOverflow.Clip` | `null` |
| `softWrap == true` | `null` |
| `maxLines == Int.MAX_VALUE` / `minLines == 1` | `null` |
| `TextDecoration.None` | `"none"` (a real wire value, distinguishable from `null` — kept faithful) |
| any other `Color` | 8-digit uppercase `#AARRGGBB` |

**Invariant (tested):** for every **canonical** model `m` in the v1 catalog,
`registry.build().create(m).toModel() == m`; for *every* model,
`create(m).toModel()` is its canonical form, renders identically to `m`, and
normalization is idempotent (applying `toModel ∘ create` twice equals applying
it once). Together with SPEC-0001 §6 this closes the full loop:
`JSON → model → widget → model → JSON` lossless for canonical v1 payloads.

## 5. Renderer & visitor cleanup

- **`RenderColumn` stops re-implementing dispatch.** Its private `when` over
  child types becomes `widget.getChildren().forEach { KomposerRenderer(it) }`.
  After this, exactly **one** render dispatch exists (`KomposerRenderer`'s
  `when` — still a `when`, by choice, until Phase 4).
- **`RenderText` starts forwarding `minLines`.** The widget field exists today
  but is never passed to Compose `Text`, so it silently does nothing —
  SPEC-0002's "every attribute visibly applied" acceptance would fail. It gains
  `minLines = widget.minLines`.
- `KomposerRenderer`'s `when` gets an `else` that throws
  `KomposerRenderException` — a widget with no render branch is a programming
  error and must fail loudly in development, not vanish.
- **`@Composable` comes off the visitor.** `KomposerWidget.Accept` and
  `KomposerWidgetVisitor.Visit` do no composition — `GraphBuilder` just builds
  a string. De-composing them lets traversal run anywhere (tests, background
  threads) and *lets* `KomposerModelDemo` hoist its graph dump out of
  recomposition — the hoist itself is a demo change (see Migration notes);
  removing `@Composable` alone doesn't stop the re-logging.

## 6. The demo becomes real

`KomposerJson2ModelDemo()` in `MainActivity.kt`:

```kotlin
@Composable
fun KomposerJson2ModelDemo() {
    val registry = remember {
        FactoryRegistry().apply {
            register<ColumnModel>(ColumnWidgetFactory())
            register<TextModel>(TextWidgetFactory())
            register<SpacerModel>(SpacerWidgetFactory())
        }
    }
    val widget = remember {
        val document = DefaultKomposerSerializer().parse(REFERENCE_JSON) // SPEC-0001 §7
        registry.build().create(document.root)
    }
    KomposerRenderer(widget)
}
```

- The JSON sample is replaced by the SPEC-0001 §7 reference payload (the old
  sample has no `type` discriminators and no envelope — it was never valid).
- `remember { }` around parse/build: deserialization must not re-run on every
  recomposition. (Now possible because no factory needs `LocalDensity`.)
- `MainActivity` switches `setContent` to `KomposerJson2ModelDemo()` — the
  JSON path becomes the primary demo; the in-memory demo stays as a secondary
  `@Preview`.

## Migration notes

- Every `Model.toWidget()` call site is rewritten to go through the registry.
- The Persian comment in `MainActivity.kt` about the registry being the single
  place to add widgets is kept — after SPEC-0003/0004 it's *closer* to true
  (schema + factory registry + renderer `when` + widget visitor + **model
  visitor** = 5 places, down from 7; SPEC-0003 promotes `KomposerModelVisitor`
  into `shared`, and its per-type overloads are a mandatory touch-point for
  every new node). Phase 4 finishes the job — a generic
  `visit(model: KomposerModel)` fallback on the model visitor is one candidate.
- `SpacerWidget.pxDp: Dp` is renamed `height: Dp` — the old name is a fossil of
  the pixel bug this spec removes.
- `KomposerModelDemo`: widget construction and the graph dump move inside
  `remember { }` (log via `SideEffect` if it should fire once per composition);
  `SpacerModel(px = LocalDensity.current.run { 16.dp.toPx() })` becomes
  `SpacerModel(height = 16f)`; `SpacerWidgetFactory` is registered without a
  `density` argument.
- `DefaultKomposerJsonFactory` (NiceToHave) is deleted: serializer + registry
  compose the same behavior without a third abstraction.

## Acceptance criteria

- [ ] `KomposerJson2ModelDemo` renders the reference payload on a device or
      emulator — a raw JSON string ends as pixels. (Screenshot in the PR.)
- [ ] Every text attribute in the reference payload is visibly applied
      (weight 700, ellipsized single line, italic nested text, 16dp gap).
- [ ] Round-trip invariant of §4 passes as a unit test over canonical variants
      of the v1 catalog, plus an idempotence test for non-canonical models.
- [ ] No `Density` needed to register factories; registry construction works
      outside composition.
- [ ] `./gradlew :androidApp:assembleDebug` and `:androidApp:lint` pass.
