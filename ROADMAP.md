# Komposer Roadmap

Deliberately **coarse**. There's real ambiguity ahead — above all, translating
the open-ended space of Compose modifiers to and from JSON — so this maps
*direction and milestones*, not tasks. Each phase has a goal and one "done
when" signal.

For the project overview and architecture, see [README.md](README.md).

---

## Phase 0 — Foundation ✅

Core abstractions and a minimal node set (Text, Column, Spacer); the in-memory
`Model → Factory → Widget → Renderer` path; a debug traversal visitor.

**Done when:** a hand-built model renders on screen. ✅ *(done — with known
seams; see "Known design tensions" in the README)*

## Phase 1 — The shared contract

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

## Phase 2 — Server-driven on screen

Rebuild the Android pipeline on the shared contract: registry keyed by
`KClass`, one recursive construction path (no more `toWidget()` bypass),
factories mapping the full Text attribute set, and `KomposerJson2ModelDemo`
finally doing what its name says.

**Done when:** a raw JSON string renders as styled pixels on a device, and
`JSON → Model → Widget → Model → JSON` is lossless for the v1 catalog's
canonical payloads.

## Phase 3 — The modifier problem *(the hard one)*

Design a serializable, **ordered** representation of styling/layout that maps
onto Compose `Modifier`. Don't boil the ocean: a small curated allow-list
(padding, size, fill, background, weight), grown deliberately. Order
matters in Compose (`padding().background()` ≠ `background().padding()`), so
the wire format must be an ordered list, not a bag of properties. Column
arrangement/alignment land here too, so layout vocabulary is designed once.
(`clickable` was originally listed here; it moved to Phase 5 —
a click without an action vocabulary would either lie on the wire or pre-empt
the event design.)

**Done when:** a widget's appearance can be meaningfully controlled from JSON
via a documented, versioned subset of modifiers. ✅ *(done — merged to
`master` via #9 / `f542d02`: shared modifier models + serialization +
`commonTest` suite green; Android fold/scope/renderer landed. The on-device
visual check is the one gate not yet run.)*

## Phase 4 — Widget catalog & simpler architecture

Grow the node set (Row, Box, Image, …) **and** dissolve
what's left of "N places to touch per widget" — not by building a better
registry but by deleting the pattern layer that made registration necessary.
`KomposerModel` is sealed (sealing *is* the registration, the trade the
modifier hierarchy already proved) and `toWidget()` extension functions plus
exhaustive `when` dispatch replace factories and visitors. Adding a node
becomes a local, additive change where every dispatch point is a branch the
**compiler** demands. Pre-render validation can return later as a plain
recursive function if needed.

**Done when:** a new widget ships by adding its own files — no registry, no
schema entry, no visitor edits; forgetting a dispatch branch is a compile
error, and the row node has landed that way as the proof.
✅ *(done — the architecture simplification landed 2026-09-03
(registry/schema/visitors/factories/composite deleted, every dispatch point a
compiler-demanded branch, wire format byte-identical), and the catalog followed
2026-09-04: `row` with `RowRenderScope`, `box` with a two-dimensional
`contentAlignment`, `image` loaded by Coil 3, and `spacing`
(`Arrangement.spacedBy`) on both `row` and `column`. Each shipped as its own
files plus the `when` branches the compiler demanded — the proof. **Button
moved to Phase 5**, for the same reason `clickable` did: a button whose tap
does nothing is a payload that lies. Lazy lists stay open (below).)*

## Phase 5 — Interactivity & state

Server-described **actions/events** (navigate, click, fetch) and a real
`KomposerState` for save/restore. UI as data eventually has to *do* something.
The `clickable` modifier deferred from Phase 3 lands here — its wire token is
already reserved — and so does the **`button` node** deferred from Phase 4
(Material 3 variants `filled|tonal|outlined|elevated|text`, a text label,
`enabled`, and the `action` this phase defines).

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
- **Catalog growth after Phase 4.** Lazy lists (`LazyColumn`/`LazyRow`: item
  keys, content types, the fold-memoization question); a per-child `align`
  modifier for `box` (the first scoped modifier after `weight`, and it needs
  `KomposerRenderScope` to grow an axis-aware `align`); a horizontal `spacer`;
  image sources beyond a URL (bundled resources), placeholders, `alpha`; the
  `aspectRatio` and `clip` modifiers that image sizing and rounded corners want.
