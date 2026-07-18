# SPEC-0007 — Single-Point Node Registration

**Status:** Proposed (2026-07-18)
**Depends on:** SPEC-0003 (schema, serializer), SPEC-0004 (registry, renderer, visitors), SPEC-0005 (render scope threading)
**Delivers:** the second half of roadmap Phase 4 — "a new widget ships by registering it in one place"

## Scope

Collapse the per-node registration surface. Today a new node touches **five**
places (SPEC-0004 migration notes; CLAUDE.md "Adding a new widget type"):

1. `KomposerSchema` — `subclass(<Node>Model::class)`
2. Factory registration at the composition root (`v1Registry()`)
3. `KomposerRenderer` — a branch in the `when`
4. `KomposerWidgetVisitor` — a `Visit` overload + `GraphBuilder`'s dispatch `when`
5. `KomposerModelVisitor` — a `visit` overload

After this spec, a new node touches **one**: a single
`KomposerNodeRegistration` handed to the registry at the composition root.
Points 1–3 are carried by the registration; points 4–5 become *optional*
(generic fallbacks with default methods — a visitor that cares about a new
type overrides its overload, but no interface edit is required to add a node).

This is engine API only. **The wire format does not change**: the same JSON
parses to the same models under the same strictness rules; every SPEC-0001/
0005 test keeps passing byte-for-byte.

## Non-goals

- **Unsealing `KomposerModifier` / a fold registry.** SPEC-0005 §4's open
  question stays open — deliberately resolved as "not now". The sealed
  hierarchy's compile-checked exhaustive fold ("adding a modifier type without
  a fold branch is a compile error") is worth more than third-party modifiers,
  for which no demand exists yet. Nodes are open; modifiers stay closed.
- Wire-format changes of any kind, including graceful unknown-node fallback
  (still Phase 6).
- A backend authoring DSL (Phase 6). The shared-side additions here
  (`KomposerNodeType`, `moduleOf`) are its enablers, not the DSL itself.
- Auto-discovery/reflection/annotation processing. Registration stays an
  explicit call at the composition root — the Persian comment in
  `MainActivity.kt` ("هر تعداد ویجت جدیدی که اضافه شد، فقط همینجا اضافه می‌شه")
  finally becomes literally true, and explicitness is why it stays auditable.

---

## 1. Shared side: `KomposerNodeType` and a buildable schema

### 1.1 The wire half of a registration

```kotlin
// shared/src/commonMain/kotlin/ir/gity/komposer/core/serialization/KomposerNodeType.kt
class KomposerNodeType<M : KomposerModel>(
    val modelClass: KClass<M>,
    val serializer: KSerializer<M>,
)

inline fun <reified M : KomposerModel> komposerNodeType(): KomposerNodeType<M> =
    KomposerNodeType(M::class, serializer())
```

Pure KMP (`KClass` + `KSerializer` both live in `commonMain`); a Kotlin
backend uses the same type to assemble its schema — this is the piece a
server needs to parse/emit a catalog that includes custom nodes.

### 1.2 `KomposerSchema` becomes buildable — and stays the v1 constant

```kotlin
object KomposerSchema {
    /** The built-in v1 catalog, now expressed through the same path custom catalogs use. */
    val v1NodeTypes: List<KomposerNodeType<*>> = listOf(
        komposerNodeType<TextModel>(),
        komposerNodeType<ColumnModel>(),
        komposerNodeType<SpacerModel>(),
    )

    val module: SerializersModule = moduleOf(v1NodeTypes)

    fun moduleOf(nodeTypes: List<KomposerNodeType<*>>): SerializersModule =
        SerializersModule {
            polymorphic(KomposerModel::class) {
                nodeTypes.forEach { subclass(it) }   // KClass + KSerializer overload
            }
        }
}
```

- `KomposerSchema.module` keeps existing (same name, same contents) — every
  current call site, test, and backend consumer compiles unchanged; it is now
  *derived* instead of hand-listed.
- Still `polymorphic(KomposerModel::class)`, never `Any::class`; still no
  `polymorphic(KomposerModifier)` block (sealed — SPEC-0005 §4's comment
  stays).
- The generic bridge (`subclass(it)` over a `KomposerNodeType<*>`) needs one
  internal star-projection helper — an implementation detail, but the spec
  names it so nobody "simplifies" the erased cast away:
  `private fun <M : KomposerModel> PolymorphicModuleBuilder<KomposerModel>.subclass(type: KomposerNodeType<M>) = subclass(type.modelClass, type.serializer)`.

### 1.3 The serializer accepts a module

```kotlin
class DefaultKomposerSerializer(
    module: SerializersModule = KomposerSchema.module,
) : KomposerSerializer { /* body unchanged; `serializersModule = module` */ }
```

Default parameter ⇒ source-compatible with every existing
`DefaultKomposerSerializer()` call. A registry-built serializer (§2) passes
its own module so wire knowledge and render knowledge can never drift apart —
the drift `KomposerSchema`'s old hand-list invited is structurally gone for
registry users.

### 1.4 `KomposerModelVisitor` grows a compiler-backed fallback

```kotlin
interface KomposerModelVisitor {
    /** Generic fallback: nodes with no dedicated overload land here. */
    fun visit(model: KomposerModel) {}

    // v1 overloads keep working — their defaults delegate to the fallback, so a
    // visitor overriding ONLY the generic method still sees every node.
    fun visit(textModel: TextModel) = visit(textModel as KomposerModel)
    fun visit(columnModel: ColumnModel) = visit(columnModel as KomposerModel)
    fun visit(spacerModel: SpacerModel) = visit(spacerModel as KomposerModel)
}
```

Why this shape (and not removing the typed overloads): Kotlin resolves
overloads statically, so `TextModel.accept`'s `visitor.visit(this)` keeps
hitting the typed overload — existing visitors that override it are
untouched. A **new** node's `accept` writes `visitor.visit(this)` and, with no
dedicated overload in the interface, statically resolves to the generic one:
adding a node no longer edits this interface. A future node *may* still add
an overload (with a delegating default) when visitors genuinely need typed
access — that's an offer, not an obligation.

The upcast in the defaults is required: `visit(textModel)` without it would
recurse into the same overload.

## 2. Android side: one registration, one registry

### 2.1 `KomposerNodeRegistration` — everything a node needs, in one value

```kotlin
// androidApp/.../core/registry/KomposerNodeRegistration.kt
class KomposerNodeRegistration<M : KomposerModel, W : KomposerWidget> @PublishedApi internal constructor(
    val nodeType: KomposerNodeType<M>,
    val widgetClass: KClass<W>,
    val factory: KomposerWidgetFactory,
    val render: @Composable (W, KomposerRenderScope?, KomposerRenderers) -> Unit,
)

inline fun <reified M : KomposerModel, reified W : KomposerWidget> nodeRegistration(
    factory: KomposerWidgetFactory,
    noinline render: @Composable (W, KomposerRenderScope?, KomposerRenderers) -> Unit,
): KomposerNodeRegistration<M, W> =
    KomposerNodeRegistration(komposerNodeType<M>(), W::class, factory, render)
```

The `M`/`W` pairing is what makes the erased cast in the renderer table (§2.3)
safe by construction: the only way to associate a render function with a
widget class is through this constructor, which forces the types to agree.

### 2.2 `KomposerRegistry` — replaces `FactoryRegistry` at the root

```kotlin
class KomposerRegistry {
    fun register(registration: KomposerNodeRegistration<*, *>): KomposerRegistry
    fun build(): Komposer
}

/** The built engine bundle: serializer, factory, and renderers that share one catalog. */
class Komposer internal constructor(
    val serializer: KomposerSerializer,       // DefaultKomposerSerializer(moduleOf(registered types))
    val factory: DefaultKomposerWidgetFactory, // KClass-keyed, snapshot at build() — SPEC-0004 §1 semantics
    val renderers: KomposerRenderers,          // §2.3
) {
    fun parse(json: String): KomposerDocument = serializer.parse(json)
    fun create(model: KomposerModel): KomposerWidget = factory.create(model)
}
```

On the name: SPEC-0003 deleted a `KomposerEngine` sketch because it added no
behavior over serializer + registry. This aggregate is not that class reborn —
it exists *because* three things (module, factories, renders) must now be
derived from one registration list, and the aggregate is the only place that
guarantee can live. Named `Komposer` (the façade a host app touches);
`build()` snapshots, exactly like `FactoryRegistry.build()` did — late
`register()` calls don't mutate a built engine.

`DefaultKomposerWidgetFactory` and the `KomposerWidgetFactory` interface are
**unchanged** — factories, their `root` recursion, and the silent-bypass fix
of SPEC-0004 §2 are untouched by this spec. `FactoryRegistry` itself is
absorbed: `KomposerRegistry` builds the same `KClass → factory` map
internally, and the standalone class is deleted (Migration notes).

### 2.3 `KomposerRenderers` — the `when` becomes a table

```kotlin
// androidApp/.../core/renderer/KomposerRenderers.kt
class KomposerRenderers internal constructor(
    private val renders: Map<KClass<out KomposerWidget>,
                             @Composable (KomposerWidget, KomposerRenderScope?, KomposerRenderers) -> Unit>,
) {
    /** Non-composable seam: dispatch + failure are unit-testable without composition. */
    fun renderFunctionFor(widget: KomposerWidget) =
        renders[widget::class] ?: throw KomposerRenderException(
            "No render registered for ${widget::class.simpleName}"
        )

    @Composable
    fun Render(widget: KomposerWidget, scope: KomposerRenderScope? = null) =
        renderFunctionFor(widget)(widget, scope, this)
}
```

- The else-throws contract survives: an unregistered widget fails loudly with
  `KomposerRenderException`, same as SPEC-0004 §5's `when`-with-else — but the
  *registered* cases can no longer drift from the catalog, because they come
  from the same registrations the factory and serializer were built from.
- `KomposerRenderer` (the top-level `when` composable) is **deleted**; this
  class is the single render dispatch. The lookup/throw path lives in a plain
  function so tests exercise dispatch without a composition — the same
  testability posture as SPEC-0005's non-composable fold.
- Composite render functions receive the `KomposerRenderers` and use it for
  children — third parameter, threaded explicitly:

```kotlin
@Composable
fun RenderColumn(widget: ColumnWidget, scope: KomposerRenderScope?, renderers: KomposerRenderers) {
    Column(
        modifier = widget.modifiers.toComposeModifier(scope),
        verticalArrangement = widget.verticalArrangement,
        horizontalAlignment = widget.horizontalAlignment,
    ) {
        val childScope = ColumnRenderScope(this)
        widget.getChildren().forEach { child -> renderers.Render(child, childScope) }
    }
}
```

**Rejected alternative — `CompositionLocal`.** A `LocalKomposerRenderers`
ambient would spare the third parameter, but a missing provider is a *runtime*
crash on first child render, and an accidentally-inherited provider from an
outer Komposer host is a silent cross-catalog leak. The explicit parameter is
compile-enforced (a custom composite *cannot* forget to forward it — the
signature demands it) and keeps render functions callable in isolation. Same
call SPEC-0005 made when it threaded `scope` explicitly instead of reaching
for an ambient.

Leaf render functions (`RenderText`, `RenderSpacer`) gain the parameter for
signature uniformity and ignore it, exactly as leaf factories ignore `root`
(SPEC-0004 §2).

### 2.4 `KomposerWidgetVisitor` — same fallback treatment as the model visitor

```kotlin
interface KomposerWidgetVisitor {
    fun Visit(widget: KomposerWidget) {}
    fun Visit(textWidget: TextWidget) = Visit(textWidget as KomposerWidget)
    fun Visit(columnWidget: ColumnWidget) = Visit(columnWidget as KomposerWidget)
    fun Visit(spacerWidget: SpacerWidget) = Visit(spacerWidget as KomposerWidget)
}
```

`GraphBuilder` drops its dispatch `when` entirely: traversal enters through
`widget.Accept(visitor)` (each widget's `Accept` calls the statically-typed
`Visit(this)`), so the generic override is no longer a second dispatch point —
it becomes `GraphBuilder`'s "unknown widget" line
(`appendLine("Unknown(${widget::class.simpleName})")`), which previously
couldn't exist at all. The `MainActivity` demo switches its entry from
`GraphBuilder().apply { Visit(widget) }` to `widget.Accept(builder)`
(Migration notes) — `Accept` was always the intended entry; the old generic
`Visit` entry only worked because it duplicated dispatch.

### 2.5 The composition root, after

```kotlin
private fun v1Komposer(): Komposer = KomposerRegistry()
    .register(nodeRegistration<TextModel, TextWidget>(TextWidgetFactory()) { w, s, r -> RenderText(w, s, r) })
    .register(nodeRegistration<ColumnModel, ColumnWidget>(ColumnWidgetFactory()) { w, s, r -> RenderColumn(w, s, r) })
    .register(nodeRegistration<SpacerModel, SpacerWidget>(SpacerWidgetFactory()) { w, s, r -> RenderSpacer(w, s, r) })
    .build()

@Composable
private fun KomposerJson2ModelDemo() {
    val komposer = remember { v1Komposer() }
    val widget = remember { komposer.create(komposer.parse(REFERENCE_JSON).root) }
    komposer.renderers.Render(widget)
}
```

Adding a node is now one `.register(...)` line. The v1 built-ins migrate onto
this path as the proof (they are also the regression suite: every SPEC-0004/
0005 rendering behavior must survive the mechanical transplant unchanged).

## 3. What "one place" means, precisely

| Concern | Before | After |
| --- | --- | --- |
| Wire schema | `KomposerSchema` hand-list | derived from registrations (built-ins keep `KomposerSchema.module`) |
| Model → Widget | `v1Registry()` registration | the same `register(...)` call |
| Widget → Compose | `KomposerRenderer` `when` branch | the registration's `render` lambda |
| Widget visitor | mandatory interface overload + `GraphBuilder` `when` | optional overload; generic default fallback |
| Model visitor | mandatory interface overload | optional overload; generic default fallback |

Mandatory places per new node: **5 → 1**. (A node-catalog spec entry is still
required — that's process, not code, and stays.)

## 4. Tests

`commonTest`:

| Test | Asserts |
| --- | --- |
| Derived module equivalence | `DefaultKomposerSerializer()` behavior is unchanged: every existing SPEC-0003/0005 round-trip, strictness, and validation test passes as-is against the now-derived `KomposerSchema.module` |
| Custom catalog | a test-only `@Serializable` model registered via `moduleOf(v1NodeTypes + komposerNodeType<TestNode>())` parses/encodes through `DefaultKomposerSerializer(module)`; the same payload against the default serializer fails with `KomposerParseException` (unknown type — strictness intact) |
| Generic visitor fallback | a visitor overriding only `visit(model: KomposerModel)` sees text/column/spacer (delegating defaults); a typed override intercepts its type only |

`androidApp` unit tests:

| Test | Asserts |
| --- | --- |
| One-place registration | a dummy node (model + widget + factory + render lambda) added via a single `register(...)`: registry-built serializer parses it, `create` builds the widget, `renderFunctionFor` returns its lambda — no edit to any interface or `when` anywhere |
| Dispatch failure | `renderFunctionFor` on an unregistered widget throws `KomposerRenderException` naming the class |
| Build snapshot | `register(...)` after `build()` does not affect the built `Komposer` (SPEC-0004 §1 semantics preserved) |
| Widget-visitor fallback | `GraphBuilder` output unchanged for the v1 tree via `Accept`; an unknown widget hits the generic line |

## Migration notes

Breaking or visible; shared first, then `androidApp`:

1. `KomposerSchema.module` is re-expressed via `v1NodeTypes`/`moduleOf` — no
   observable change; `DefaultKomposerSerializer` gains the module parameter
   (default preserves behavior).
2. `KomposerModelVisitor` / `KomposerWidgetVisitor` gain generic fallbacks;
   existing typed overloads become default methods. Existing implementors
   compile unchanged (they override the same signatures).
3. **`FactoryRegistry` is deleted**, absorbed by `KomposerRegistry`.
   `v1Registry()` in `MainActivity.kt` becomes `v1Komposer()` (§2.5); its
   Persian comment stays — now literally true.
4. **`KomposerRenderer` (the `when` composable) is deleted**, replaced by
   `KomposerRenderers.Render`. Every `Render*` function gains the
   `renderers: KomposerRenderers` parameter; `RenderColumn` recurses through
   it. SPEC-0004 §5's "one render dispatch" invariant transfers to the table.
5. `GraphBuilder` drops its dispatch `when`; the `KomposerModelDemo` graph-dump
   entry switches to `widget.Accept(builder)`.
6. SPEC-0006, if implemented after this spec, registers `row` as one
   `nodeRegistration<RowModel, RowWidget>(...)` line instead of its §4
   five-point list.
7. Root docs: CLAUDE.md's "Adding a new widget type" five-point checklist and
   the roadmap's Phase 4 friction paragraph are rewritten against §3's table
   in the implementing PR.

## Acceptance criteria

- [ ] The roadmap's Phase 4 "done when" holds and is unit-tested: a new
      (test-only) widget ships by registering it in **one** place, with no
      edits to `KomposerSchema`, any `when`, or any visitor interface.
- [ ] Every pre-existing `commonTest` and `androidApp` test passes unchanged —
      the wire format and rendering behavior are byte-for-byte identical.
- [ ] All §4 tests pass via `:shared:testDebugUnitTest` and
      `:androidApp:testDebugUnitTest`.
- [ ] The reference payload demo renders identically before/after the
      transplant (screenshot pair in the PR).
- [ ] `grep -rE 'import (androidx|android|java)\.' shared/src/commonMain/`
      returns nothing (`KomposerNodeType` and `moduleOf` are pure KMP).
- [ ] `./gradlew :androidApp:assembleDebug` and `:androidApp:lint` pass.

## Open questions (deliberately deferred)

- **Modifier extensibility** — deliberately not unsealed here (Non-goals);
  reopen only if a third-party modifier demand actually materializes.
- **Registration-time catalog validation** — `build()` could verify wire-token
  uniqueness across registrations (two nodes claiming `"type": "card"`), which
  kotlinx.serialization would otherwise surface late. Cheap to add; needs a
  decision on failing fast vs. last-wins.
- **Per-registration metadata** — SPEC-0008 wants "does this node provide a
  weight scope?" as a declarable fact; a `providesWeightScope: Boolean` (or a
  scope-factory slot) on `KomposerNodeRegistration` is the natural home once
  both specs are accepted.
- **`Komposer` as the Phase 6 backend seam** — a JVM backend needs only the
  shared half (`KomposerNodeType`, `moduleOf`, serializer); whether a
  shared-side "catalog" aggregate (module + validation, no rendering) is worth
  naming waits for the `jvm()` target.
