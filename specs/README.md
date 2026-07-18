# Komposer Specs

Exact, implementation-ready specifications. Where [ROADMAP.md](../ROADMAP.md) gives
direction, a spec gives **decisions**: field names, types, defaults, error behavior,
and acceptance criteria precise enough that implementation is mostly transcription.

| Spec | Title | Covers | Roadmap phase |
| --- | --- | --- | --- |
| [0001](0001-json-wire-format.md) | JSON wire format v1 | Envelope, `type` discriminator, scalar conventions (dp/sp/colors/enums), strictness rules | Phase 1 |
| [0002](0002-node-catalog-v1.md) | Node catalog v1 | Exact fields for `text` (rich), `column`, `spacer`; validation; Compose mapping | Phase 1 |
| [0003](0003-model-layer-and-serialization.md) | Model layer & serialization in KMP | Moving models to `shared/commonMain`, purifying `KomposerModel`, the real serializer, tests | Phase 1 |
| [0004](0004-android-rendering-pipeline.md) | Android rendering pipeline | `KClass` registry, factory recursion, renderer cleanup, working JSON demo | Phase 2 |
| [0005](0005-modifier-system.md) | Modifier system v1 | Ordered `modifiers` list, the v1 allow-list (padding/size/fill/background/weight), column arrangement/alignment, the render-time fold | Phase 3 |
| [0006](0006-row-node.md) | Row node & spacing | The `row` node (per-axis enums, `RowRenderScope` for weight), the `spacing` field on `row`+`column` (the deferred `spacedBy` decision) | Phase 4 |
| [0007](0007-node-registration.md) | Single-point node registration | `KomposerNodeRegistration` + `KomposerRegistry` collapsing the five per-node touch-points to one; generic visitor fallbacks; renderer `when` → table | Phase 4 |
| [0008](0008-prerender-validation.md) | Pre-render document validation | `KomposerValidator` collect-all semantic pass (weight placement, SPEC-0005's deferred question) — the model visitor's first real job | Phase 4 |

## Status & lifecycle

Each spec carries a status:

- **Proposed** — written, awaiting review; may change freely.
- **Accepted** — agreed; changes require touching the spec first.
- **Implemented** — code matches the spec; the spec becomes documentation.

Specs 0001–0005 are **Implemented** (2026-07-17): the model + serialization
layer lives in `shared/commonMain`, the Android pipeline renders the reference
payload from raw JSON, and SPEC-0005's ordered modifier system is in on both
sides of the split (shared first, then `androidApp`, merged via #9 / `f542d02`).
Two deferred checks remain: iOS *test execution* (the shared module compiles
for all iOS targets; running the KMP tests needs full Xcode), and SPEC-0005's
device/`assembleDebug`/`lint` acceptance criteria (SPEC-0005 §10) have not
been run (attempted 2026-07-18: the remote environment's egress policy denies
`dl.google.com` — Google Maven, needed for AGP + Compose — and
`maven.myket.ir`, so no Gradle task can resolve dependencies there; run the
gate locally or allowlist those hosts).

Phase 4's specs are now open, all **Proposed**: [0006](0006-row-node.md)
picks up the Row seam SPEC-0005's forward notes recorded (plus the deferred
`spacedBy` decision), [0007](0007-node-registration.md) attacks the
"five places to touch per node" friction — the phase's other half — and
[0008](0008-prerender-validation.md) answers SPEC-0005's deferred
weight-placement-validation question with a shared, collect-all
`KomposerValidator`. They may be implemented in any order; each records its
interplay with the others. Phases 5–6 stay deliberately spec-less per the
roadmap's philosophy; their seams (`clickable` → Phase 5, `jvm()` target →
Phase 6) remain recorded in SPEC-0005's open questions rather than in
premature specs of their own.

## Suggested implementation order

1. **0003** is the workhorse: it moves the models and implements serialization.
   0001 and 0002 are the *contracts* that 0003 implements — read them first,
   implement them inside 0003's structure.
2. **0004** then rebuilds the Android side on top of the shared contract and makes
   the JSON demo real.
3. **0005** splits the same way its predecessors did: modifier models,
   validation, and round-trip tests in `shared` first; then the fold, scope,
   and renderer changes in `androidApp`.

Each spec ends with acceptance criteria. A spec is not done until all of its
criteria pass.

## Conventions used in specs

- **"wire"** means the JSON representation; **"model"** means the Kotlin
  `@Serializable` class; **"widget"** means the Compose-aware client object.
- Optional wire field ⇒ nullable Kotlin property defaulting to `null`; the
  *mapping layer* (factory), not the model, applies Compose defaults.
- Breaking changes to existing code are listed explicitly under **Migration notes**.
