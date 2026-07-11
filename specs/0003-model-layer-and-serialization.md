# SPEC-0003 — Model Layer & Serialization in KMP

**Status:** Proposed
**Depends on:** SPEC-0001 (wire format), SPEC-0002 (node fields)
**Enables:** SPEC-0004 (Android pipeline), the future Kotlin backend

## Scope

Move the model layer out of `androidApp` into `shared/commonMain`, make
`KomposerModel` pure data, and implement real polymorphic JSON serialization
with tests that run on every KMP target. This is the keystone move: after it,
the *same types* that describe UI on the client are available to a Kotlin
backend.

## Non-goals

- Widgets, factories, renderers — they stay in `androidApp` (SPEC-0004).
- Compose Multiplatform / iOS rendering — the iOS win here is only that
  `shared` keeps compiling for iOS targets with the models inside.

---

## 1. Target layout

```
shared/src/commonMain/kotlin/ir/gity/komposer/core/
├── model/
│   ├── KomposerModel.kt            # purified interface (§2)
│   ├── KomposerModelVisitor.kt     # moved out of NiceToHave.kt
│   ├── KomposerDocument.kt         # envelope (§4)
│   ├── text/TextModel.kt           # + the four enum value classes
│   ├── column/ColumnModel.kt
│   └── spacer/SpacerModel.kt
└── serialization/
    ├── KomposerSchema.kt           # SerializersModule — THE registration point (§3)
    ├── KomposerSerializer.kt       # interface (§5)
    ├── DefaultKomposerSerializer.kt
    └── KomposerParseException.kt

shared/src/commonTest/kotlin/ir/gity/komposer/core/serialization/
├── RoundTripTest.kt
├── StrictnessTest.kt
└── ValidationTest.kt
```

Packages keep their current names, so this is a *move*, not a rename — the
`androidApp` copies are **deleted in the same commit** (same package + class
names in two modules would otherwise collide on the Android classpath).

## 2. Purifying `KomposerModel`

```kotlin
// commonMain — no Compose, no widget, no java.* imports allowed in this layer
interface KomposerModel {
    fun accept(visitor: KomposerModelVisitor)
}
```

**`toWidget()` is removed from the interface and from every model.**

Rationale — this is the central decision of the spec:

- Widgets are Compose-coupled; a model that can construct one can never live in
  `commonMain`.
- Today there are **two competing construction paths**: `Model.toWidget()` and
  the factory registry. Worse, they're entangled — `ColumnWidgetFactory.create`
  calls `children.map { it.toWidget() }`, so a custom factory registered for
  `TextModel` is silently bypassed for texts inside a column. Removing
  `toWidget()` leaves exactly one path: **factories** (recursion fix in
  SPEC-0004 §2).
- `accept` stays: it's dependency-free, and `KomposerModelVisitor` (promoted
  out of `NiceToHave.kt`) becomes useful server-side too (validation passes,
  payload statistics) — a visitor that works where Compose doesn't exist.

`KomposerModel` remains a non-sealed interface: third-party nodes register via
the schema/registry rather than by editing a sealed hierarchy. (Revisit if
auto-registration ever matters more than extensibility.)

## 3. `KomposerSchema` — the single registration point

```kotlin
object KomposerSchema {
    val module = SerializersModule {
        polymorphic(KomposerModel::class) {
            subclass(TextModel::class)
            subclass(ColumnModel::class)
            subclass(SpacerModel::class)
            // New node types register here.
        }
    }
}
```

Fixes two existing defects in `DefaultKomposerSerializer`'s sketch:

- `polymorphic(Any::class)` → `polymorphic(KomposerModel::class)`. Registering
  against `Any` never matches properties typed `KomposerModel`.
- `ColumnModel.children` drops `@Contextual`. Interface-typed properties are
  polymorphic by default once the base class is registered; `@Contextual`
  actively routes them away from that.

`KomposerSchema` is the wire-level half of the "one place to register a node"
goal (roadmap Phase 4); the render-level half is the factory registry.

## 4. The envelope model

```kotlin
@Serializable
data class KomposerDocument(
    val version: Int,          // no default — required on the wire, always encoded
    val root: KomposerModel,
)
```

Parsing a document with `version != 1` throws `KomposerParseException`
(checked in `init`).

## 5. Serializer API

```kotlin
interface KomposerSerializer {
    fun encode(document: KomposerDocument): String
    fun parse(json: String): KomposerDocument
    fun encodeNode(model: KomposerModel): String   // mostly for tests/tools
    fun parseNode(json: String): KomposerModel
}
```

Changes from the current sketch in `NiceToHave.kt`:

- **No `Class<T>` parameter.** `deserialize(json, clazz)` was doubly wrong:
  `java.lang.Class` doesn't exist in `commonMain`, and polymorphic parsing
  makes the caller-supplied class unnecessary — the `type` field decides.
- Names change `serialize`/`deserialize` → `encode`/`parse` to match
  kotlinx.serialization vocabulary and to signal the API break loudly.

Implementation:

```kotlin
class DefaultKomposerSerializer : KomposerSerializer {
    private val json = Json {
        serializersModule = KomposerSchema.module
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = false
    }
    // Every parse/encode wraps SerializationException and
    // IllegalArgumentException (from model init validation) into
    // KomposerParseException(message, cause).
}
```

```kotlin
class KomposerParseException(message: String, cause: Throwable? = null)
    : Exception(message, cause)
```

One exception type for "the payload is bad" gives callers a single catch point
regardless of whether kotlinx or an `init` check rejected it.

## 6. Tests (`commonTest`, run on all targets)

| Test | Asserts |
| --- | --- |
| Round-trip, minimal | For each node type with only required fields: `parseNode(encodeNode(m)) == m` |
| Round-trip, full | `TextModel` with **every** field set round-trips |
| Round-trip, document | SPEC-0001 §7 reference payload: `parse` → assert tree field-by-field → `encode` → `parse` → equal |
| Nested tree | Column in column preserves order and depth |
| Unknown type | `{"type":"blink", ...}` → `KomposerParseException` |
| Unknown field | `{"type":"text","text":"hi","glow":true}` parses fine |
| Missing required | text without `text`, spacer without `height`, document without `version` → fail |
| Bad values | Each SPEC-0002 §4 rule → `KomposerParseException` |
| Encoding minimality | Default text node encodes with no optional keys, no `null`s |

Run with `./gradlew :shared:allTests` (iOS-target tests require macOS;
`:shared:testDebugUnitTest` is the everyday loop).

## Migration notes (breaking, all in one commit ideally)

1. Move + purify models; delete `androidApp/.../core/model/**`.
2. `TextModel.text` becomes required `String` (was `String? = null`).
3. `SpacerModel.px: Float` becomes `height: Float` in dp.
4. Delete from `NiceToHave.kt`: `KomposerSerializer`, `DefaultKomposerSerializer`,
   `KomposerModelVisitor` (moved), `DefaultKomposerJsonFactory`,
   `DefaultKomposerMapper`, `KomposerWidgetMapper` (the factory registry *is*
   the mapper — two stub mapper classes duplicating it are deleted, not
   finished). `KomposerEngine`, `KomposerState`, `Specification` stay parked.
5. `androidApp` compiles against the shared models (it already depends on
   `:shared`); every `it.toWidget()` call site breaks → fixed by SPEC-0004 §2.

## Acceptance criteria

- [ ] `shared` compiles for `androidTarget`, `iosX64`, `iosArm64`,
      `iosSimulatorArm64` with the model + serialization layer inside.
- [ ] No Compose, Android, or `java.*` import anywhere under
      `shared/src/commonMain/.../core/`.
- [ ] All §6 tests pass via `:shared:testDebugUnitTest` (and `:shared:allTests`
      on macOS).
- [ ] `androidApp` builds and `KomposerModelDemo` still renders, now consuming
      shared models.
