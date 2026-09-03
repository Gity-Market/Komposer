# Compose Remote vs Komposer — an architecture comparison

> **Dated snapshot.** This is kept as written, not rewritten. It cites `SPEC-000X §Y`
> section numbers from the `specs/` directory that Komposer carried through Phase 3;
> those documents were removed when the Phase 4 architecture simplification landed and
> now live only in git history. Every wire-format conclusion below still holds — the
> modifier system it reviews shipped as described, and Phase 4 changed no wire format.
> What did change is the client-side pipeline: "Model → factory → widget" below is now
> "Model → `toWidget()` → widget", with the factory and visitor layers deleted. See
> [README.md](../README.md) for the current architecture.

Written 2026-07-17, as a pre-implementation review for SPEC-0005 (modifier system,
Phase 3). Compose Remote (`androidx.compose.remote`, historically "RemoteCompose")
was at `1.0.0-alpha15` at the time of writing; its APIs are explicitly unstable, so
this note records the *design decisions* observed, not API signatures to depend on.

**TL;DR:** the two projects solve server-driven UI at deliberately different
altitudes, and Komposer should not chase Compose Remote's operation-stream
approach. Where the problems overlap — above all the modifier system — Compose
Remote independently arrives at the same structure SPEC-0005 specifies. SPEC-0005
is implemented as written, with no structural changes; the transferable lessons
are recorded here and as prior-art notes in the spec's open questions.

## What Compose Remote is

Compose Remote is the AndroidX team's framework for serializing UI into a portable
**binary document** and rendering it natively on another device without shipping the
original code. Its shape:

- **Creation side** (`remote-creation-core` / `-jvm` / `-android` / `-compose`):
  Compose-mirroring APIs (`RemoteColumn`, `RemoteText`, `RemoteBox`,
  `RemoteModifier`, `@RemoteComposable`) whose output is not pixels but a
  `RemoteComposeBuffer` — a stream of serialized operations. The core creation
  artifacts run on a plain JVM with no Android SDK, so documents can be generated
  in a backend service.
- **The document**: 90+ typed operations at roughly display-list altitude —
  Canvas draw primitives (`DRAW_RECT`, `DRAW_TEXT`, `DRAW_BITMAP`, …), layout
  operations with a push/pop container model (`Component`, `Container` /
  `ContainerEnd`, `LoopOperation`), modifier operations (padding, background,
  border, click, …), state and expression operations (`NamedVariable`,
  `FloatExpression` evaluated per frame, `ConditionalOp`, time references), and
  interaction operations (touch regions mapped to named actions).
- **Player side** (`remote-player-core` / `-view`, plus a composable player): an
  interpreter that executes operations against a Canvas, evaluates expressions
  each frame, and fires named action strings to a host-app callback.

The document is self-contained: the client needs no schema knowledge, no component
registry, and no code from the producer — it is a dumb executor. That is the
source of both its power (full visual fidelity, forward compatibility, custom
drawing for free) and its costs (opaque documents that are hard to inspect, diff,
validate, or author by hand; a Kotlin toolchain required to produce them).

## Where the architectures differ — deliberately

| | **Compose Remote** | **Komposer** |
| --- | --- | --- |
| Wire format | Binary operation stream, near display-list | Versioned JSON **semantic component tree** (`text`, `column`, `spacer`) |
| Altitude | "Draw a rounded rect here, then this text there" | "This is a text inside a column" |
| Contract | None needed — both ends ship the same library version | The schema **is** the contract: strict, versioned (SPEC-0001), human-authorable and inspectable |
| Producer | Kotlin/JVM code through capture APIs | Any JSON producer today; typed backend DSL is roadmap Phase 6 |
| Unknown vocabulary | Impossible by construction (ops are too low-level to be "unknown") | Unknown node/modifier `type` fails loudly (`KomposerParseException`); graceful fallback is deliberately Phase 6 |
| Validation | Not applicable — trusted producer, trusted bytes | Parse-time `require` validation of every field (SPEC-0002 §4) |
| State / interactivity | Named variables + per-frame expression evaluation; `clickable(action = "name")` → host `onAction` callback | Phase 5 (token reserved, SPEC-0005 §2.6) |
| Rendering | Operation interpreter over Canvas | Model → factory → widget → `@Composable` dispatch (SPEC-0004) |

A useful way to hold the whole comparison: **Compose Remote is a portable
recording of Compose *output*; Komposer is a portable description of Compose
*input*.** A recording maximizes fidelity and forward compatibility; a description
maximizes inspectability, validation, and independence from any particular
producer. These optimize for different products, and Komposer's specs
(strictness, round-trip losslessness, wire-first design) are all commitments to
the second. Adopting an operation stream would not be a refactor — it would be a
different project.

## The modifier double-check (SPEC-0005 vs `RemoteModifier`)

The specific question this review set out to answer: does SPEC-0005's modifier
structure hold up against Google's? Point for point:

1. **An ordered chain of typed modifier records attached to a node — both.**
   `RemoteModifier` mirrors Compose's `Modifier` chain
   (`RemoteModifier.fillMaxWidth().background(…).padding(16f).clickable(…)`) and
   serializes as ordered modifier operations inside the owning component.
   SPEC-0005 §1's ordered JSON array of `type`-discriminated objects is the same
   structure expressed in JSON. Both treat **order as semantics** and preserve it
   on the wire.
2. **Modifiers are node data, not wrapper nodes — both.** Neither project wraps
   children Flutter-style; a component owns its modifier list. SPEC-0005's
   "visitors untouched, modifiers are node data" matches.
3. **Mirror Compose's vocabulary 1:1 — both.** `RemoteModifier`'s extension names
   are Compose's names. SPEC-0005 follows the same rule (e.g. §2.3's three
   `fillMax*` types instead of an invented `fill` + direction enum).
4. **`start`/`end`, not `left`/`right`.** Compose Remote shipped `left`/`right`
   padding and had to break the API in alpha09 for RTL support. SPEC-0005 §2.1
   uses `start`/`top`/`end`/`bottom` from day one — validated by their scar.
5. **Arrangement/alignment are component parameters, not modifiers — both.**
   `RemoteRow(horizontalArrangement = …, verticalAlignment = …)` mirrors Compose's
   own split, exactly as SPEC-0005 §3 puts `verticalArrangement` /
   `horizontalAlignment` on the `column` node rather than in the modifier list.
6. **`clickable` is a modifier carrying a named action, handled by a host
   callback — and it took them several alphas to settle.** Their evolution
   (varargs actions → a single `Action` with `CombinedAction` for aggregation,
   plus `HostAction` and `PendingIntentAction` variants) is precisely the
   action-vocabulary design problem SPEC-0005 §2.6 deferred `clickable` to
   Phase 5 for. The deferral is validated, and their landing point is the proven
   prior art for Phase 5.
7. **dp on the wire, density resolved on the player — both.** Their
   `RemoteDensityBehavior` defaults to player-side density resolution, matching
   SPEC-0001's dp-number convention and device-side conversion.
8. **A pure-JVM creation side — both (Komposer: planned).** Their
   `remote-creation-core`/`-jvm` split validates the KMP purity rule for
   `shared/commonMain` and the Phase 6 `jvm()` target + backend DSL plan.

Where they genuinely differ on modifiers is **catalog breadth, not structure**:
Compose Remote already carries `border` (with a corner radius), `clip`, `alpha`,
`rotate`/`scale`, `semantics`, and low-level touch handlers. Komposer's v1
allow-list (padding, size, fill, background, weight) is deliberately small and
grows through SPEC-0005 §2.7's checklist — the divergence is pace, by design.

One structural point with no counterpart on their side: Komposer's `weight`
scoping (`KomposerRenderScope`, SPEC-0005 §5.3). Compose Remote's constraint-based
layout world doesn't surface this problem; Komposer's solution is taken directly
from Compose's own `ColumnScope`/`RowScope` receiver design, which is the right
authority for it.

**Verdict: implement SPEC-0005 exactly as specced.** Nothing observed in Compose
Remote argues for changing the wire shape, the sealed hierarchy, the fold, or the
scope mechanism.

## Transferable lessons, by roadmap phase

- **Phase 3 (now):** none required — the review found alignment, not defects.
- **Phase 4 (widget catalog, registration friction):** Compose Remote's
  operations are self-describing records dispatched through an op-code registry —
  a single registration point per operation. That is the direction Phase 4's
  "collapse the five touch-points" already aims at; their design confirms a
  registry keyed by discriminator, with serialization and dispatch resolved from
  one registration, is the standard resolution.
- **Phase 5 (interactivity/state):** the strongest import. Their model —
  named action strings in the document, one host-side `onAction` callback,
  `CombinedAction` for multi-action gestures, and named-variable state
  (`NamedVariable` on the wire, `updateVariable` from the client, conditionals
  over variables in the document) — keeps the document ignorant of the host's
  navigation/analytics/state implementations. It matches the parked
  `KomposerState` sketch in `NiceToHave.kt` and should be the starting point for
  the Phase 5 spec. Their per-frame expression language (`FloatExpression`,
  time-based animation) is a further step Komposer need not take in Phase 5, but
  it explains a wire-format consideration: their scalar values may be literals
  *or* variable references. Komposer's typed `Float` fields are literals only —
  if dynamic values ever arrive, the wire needs a scalar-or-reference union,
  which is a versioned-format question to remember, not a v1 change.
- **Phase 6 (jvm target, graceful fallback):** their pure-JVM creation artifacts
  validate the plan. One addition: when graceful unknown-*node* fallback lands,
  decide the unknown-*modifier* policy (skip-with-warning vs fail) at the same
  time. Compose Remote never faces this — both ends version together — but
  Komposer's wire contract does, and today's sealed-hierarchy strictness (unknown
  modifier ⇒ `KomposerParseException`) is only the right answer while v1 is
  strict everywhere.
- **Catalog growth (no phase):** their shipped modifier set suggests candidates
  beyond SPEC-0005's list: `semantics`/`contentDescription` (accessibility —
  currently absent from Komposer's roadmap entirely, and worth a phase home),
  and `rotate`/`scale` alongside the already-listed `alpha`. For the deferred
  shape vocabulary, their minimal story — `border(width, color, radius)` and a
  scalar corner radius before per-corner control — is a sensible v-next shape.

## What Komposer deliberately does not adopt

- **The operation stream / binary format.** Costs Komposer's core properties:
  hand-authorable payloads, `git diff`-able UI, parse-time validation, and a
  producer-independent contract.
- **"No schema" forward compatibility.** Their claim that new designs run on old
  clients holds *because* operations are below the semantic level. A semantic
  tree cannot have that property; Komposer's answer is versioning plus Phase 6's
  explicit fallback — a different, honest trade.
- **Capture-based creation** (`@RemoteComposable` interception). Komposer's
  Phase 6 DSL builds model trees directly; it needs no compiler plugin or
  composition capture because the model layer *is* the document.
- **Per-frame expression evaluation.** Powerful, but it turns the client into an
  interpreter with an embedded language — far beyond Komposer's declared scope.

## Sources

- [Compose Remote releases — developer.android.com](https://developer.android.com/jetpack/androidx/releases/compose-remote)
  (module list, version history including the alpha09 `left/right` → `start/end`
  padding migration and the alpha12 single-`Action` modifier API).
- [RemoteCompose: Another Paradigm for Server-Driven UI in Jetpack Compose — Jaewoong Eum, ProAndroidDev](https://proandroiddev.com/remotecompose-another-paradigm-for-server-driven-ui-in-jetpack-compose-92186619ba8f)
  (operation model, creation/playback split, state & expressions; full text reviewed).
- [Remote Compose: Server-Driven UI — Nativeblocks blog](https://nativeblocks.io/blog/remote-compose-android-server-driven-ui/)
  (end-to-end example with `RemoteModifier` chains, modifier operations, action
  handling; full text reviewed).
- [Introducing RemoteCompose: break your UI out of the app sandbox — talk by the library authors](https://speakerdeck.com/camaelon/introducing-remotecompose-break-your-ui-out-of-the-app-sandbox).
