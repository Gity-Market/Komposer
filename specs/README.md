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

## Status & lifecycle

Each spec carries a status:

- **Proposed** — written, awaiting review; may change freely.
- **Accepted** — agreed; changes require touching the spec first.
- **Implemented** — code matches the spec; the spec becomes documentation.

All four specs are **Implemented** (2026-07-12): the model + serialization layer
lives in `shared/commonMain`, and the Android pipeline renders the reference
payload from raw JSON. The one deferred check is iOS *test execution* (compiles
for all iOS targets; running the KMP tests needs full Xcode).

## Suggested implementation order

1. **0003** is the workhorse: it moves the models and implements serialization.
   0001 and 0002 are the *contracts* that 0003 implements — read them first,
   implement them inside 0003's structure.
2. **0004** then rebuilds the Android side on top of the shared contract and makes
   the JSON demo real.

Each spec ends with acceptance criteria. A spec is not done until all of its
criteria pass.

## Conventions used in specs

- **"wire"** means the JSON representation; **"model"** means the Kotlin
  `@Serializable` class; **"widget"** means the Compose-aware client object.
- Optional wire field ⇒ nullable Kotlin property defaulting to `null`; the
  *mapping layer* (factory), not the model, applies Compose defaults.
- Breaking changes to existing code are listed explicitly under **Migration notes**.
