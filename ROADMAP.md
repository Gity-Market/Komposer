# Komposer Roadmap

Deliberately **coarse**. There's a lot of ambiguity ahead — especially around
translating the full space of Compose modifiers to and from JSON — so this maps
*direction and milestones*, not tasks. Each phase lists its goal and a single
"done when" signal. Phases roughly build on each other but aren't strictly serial.

For the project overview and architecture, see [README.md](README.md).

---

## Phase 0 — Foundation · *largely in place*

The core abstractions and a minimal node set (Text, Column, Spacer), the in-memory
`Model → Factory → Widget → Renderer` path, and a debug traversal visitor.

**Done when:** a hand-built model renders on screen. ✅

## Phase 1 — Close the JSON loop

Make `(de)serialization` real: polymorphic JSON ⇄ Model so a server payload renders
end-to-end. This is the keystone — it's what makes the thing actually *server-driven*.

**Done when:** `KomposerJson2ModelDemo()` renders from a raw JSON string, and
`Model → JSON → Model` round-trips losslessly.

## Phase 2 — Go multiplatform

Lift the engine out of `androidApp` into `shared/commonMain`. Models and serialization
become pure Kotlin (no Compose imports); Compose-coupled rendering stays in the
platform layer. This is the unlock for a **shared Kotlin backend** that emits the very
same Model types.

**Done when:** the model + serialization layer compiles in `commonMain` and is consumed
unchanged by both the Android and iOS hosts.

## Phase 3 — The modifier problem *(the hard one)*

Design a serializable, **ordered** representation of styling/layout that maps to
Compose `Modifier`. Don't boil the ocean: start with a small, curated allow-list
(padding, size, background, weight, clickable) and grow it deliberately. Order matters
in Compose, so the wire format must preserve it.

**Done when:** a widget's appearance can be meaningfully controlled from JSON via a
documented, versioned subset of modifiers.

## Phase 4 — Widget catalog & lower friction

Grow the node set (Row, Box, Image, Button, lazy lists, …) **and** collapse the
"seven places to add a widget" into a single registration. Adding a node should be a
local, additive change — no editing of central `when` blocks.

**Done when:** a new widget is added by registering it in one place.

## Phase 5 — Interactivity & state

Server-described **actions/events** (navigate, click, fetch) and a real `KomposerState`
for save/restore. UI as data has to eventually *do* something.

**Done when:** a JSON-described button can trigger a defined action and survive
configuration changes.

## Phase 6 — Backend & tooling

A Kotlin DSL/builders for producing Models server-side, schema **versioning** &
forward/backward compatibility, payload validation, and authoring previews.

**Done when:** the backend can construct and serve a screen using shared Kotlin types
with validation, and the client renders it with graceful fallback for unknown nodes.
