# Komposer Roadmap

Deliberately **coarse**. There's real ambiguity ahead — above all, translating
the open-ended space of Compose modifiers to and from JSON — so this maps
*direction and milestones*, not tasks. Each phase has a goal and one "done
when" signal. Near-term phases (1–3) are backed by exact specs in
[`specs/`](specs/); later phases intentionally are not, because writing
detailed plans into ambiguity just creates plans to throw away.

For the project overview and architecture, see [README.md](README.md).

---

## Phase 0 — Foundation ✅

Core abstractions and a minimal node set (Text, Column, Spacer); the in-memory
`Model → Factory → Widget → Renderer` path; a debug traversal visitor.

**Done when:** a hand-built model renders on screen. ✅ *(done — with known
seams; see "Known design tensions" in the README)*

## Phase 1 — The shared contract *(specs [0001](specs/0001-json-wire-format.md), [0002](specs/0002-node-catalog-v1.md), [0003](specs/0003-model-layer-and-serialization.md))*

Make the Model layer what it claims to be: pure serializable data, living in
`shared/commonMain`, with a real polymorphic JSON round-trip and a versioned
wire format. Text gets its full v1 attribute set (maxLines, fontWeight, color,
…) here, because the wire format needs one rich node to prove itself against.

This phase deliberately lands **before** the on-screen JSON demo (they were
ordered the other way around previously). Reason: the serialization work
forces API decisions — `KClass` vs `Class`, no Compose imports, no
platform types — and doing it inside `androidApp` first would mean making
those decisions twice. It's also the unlock for the whole KMP-first goal: a
Kotlin backend emitting the very same types.

**Done when:** the model + serialization layer compiles for all KMP targets,
and `JSON ⇄ Model` round-trips losslessly under `commonTest` on every target
(everyday loop: `:shared:testDebugUnitTest`; the iOS targets run via
`:shared:allTests` on macOS).

## Phase 2 — Server-driven on screen *(spec [0004](specs/0004-android-rendering-pipeline.md))*

Rebuild the Android pipeline on the shared contract: registry keyed by
`KClass`, one recursive construction path (no more `toWidget()` bypass),
factories mapping the full Text attribute set, and `KomposerJson2ModelDemo`
finally doing what its name says.

**Done when:** a raw JSON string renders as styled pixels on a device, and
`JSON → Model → Widget → Model → JSON` is lossless for the v1 catalog's
canonical payloads ([SPEC-0004 §4](specs/0004-android-rendering-pipeline.md)).

## Phase 3 — The modifier problem *(the hard one — spec [0005](specs/0005-modifier-system.md))*

Design a serializable, **ordered** representation of styling/layout that maps
onto Compose `Modifier`. Don't boil the ocean: a small curated allow-list
(padding, size, fill, background, weight), grown deliberately. Order
matters in Compose (`padding().background()` ≠ `background().padding()`), so
the wire format must be an ordered list, not a bag of properties. Column
arrangement/alignment land here too, so layout vocabulary is designed once.
(`clickable` was originally listed here; SPEC-0005 §2.6 moves it to Phase 5 —
a click without an action vocabulary would either lie on the wire or pre-empt
the event design.)

**Done when:** a widget's appearance can be meaningfully controlled from JSON
via a documented, versioned subset of modifiers. 🚧 *(in progress — SPEC-0005
implemented on the feature branch: shared modifier models + serialization +
`commonTest` suite green; Android fold/scope/renderer landed. Still Accepted,
not Implemented, until the device/`assembleDebug`/`lint` acceptance criteria run
in a Google-Maven-capable environment.)*

## Phase 4 — Widget catalog & lower friction

Grow the node set (Row, Box, Image, Button, lazy lists, …) **and** collapse
what's left of "N places to touch per widget" into a single registration.
Phases 1–2 already reduce seven places to five (schema, factory registry,
renderer `when`, widget visitor, model visitor); this phase attacks the rest —
a generic `visit(model)` fallback on the model visitor is one candidate — so
adding a node is a local, additive change.

**Done when:** a new widget ships by registering it in one place.

## Phase 5 — Interactivity & state

Server-described **actions/events** (navigate, click, fetch) and a real
`KomposerState` for save/restore. UI as data eventually has to *do* something.
The `clickable` modifier deferred from Phase 3 lands here — its wire token is
already reserved ([SPEC-0005 §2.6](specs/0005-modifier-system.md)).

**Done when:** a JSON-described button triggers a defined action and the
screen survives configuration changes.

## Phase 6 — Backend & tooling

First enabler, deliberately deferred until here: **add a `jvm()` target to
`shared/build.gradle.kts`** — today `shared` compiles only for Android and the
three iOS targets, so no Kotlin/JVM backend can even depend on the shared
models. It's a one-line change, kept out of Phase 1 so an untested target isn't
carried through the contract-stabilization phases.

Then: a Kotlin DSL for producing Models server-side (a sample Ktor endpoint
emitting shared types would be the proof), schema versioning and
forward/backward compatibility (including graceful fallback for unknown node
types — kept deliberately strict until here), payload validation, authoring
previews.

**Done when:** a backend (on the new `jvm()` target) constructs and serves a
screen using shared Kotlin types, and the client renders it with graceful
fallback for unknown nodes.

---

## Explicitly open (no phase yet)

- **iOS rendering.** `shared` compiles for iOS, but nothing renders there.
  Compose Multiplatform vs. a native SwiftUI renderer over the same models is
  a genuinely open call — deferred until the contract is stable.
- **Theming.** Colors as raw hex are v1 pragmatism; theme references
  (`"primary"`) need a real theming story.
- **Non-Kotlin backends.** A generated JSON Schema would decouple the wire
  format from Kotlin; only worth it if a non-Kotlin producer actually appears.
