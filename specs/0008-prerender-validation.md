# SPEC-0008 — Pre-Render Document Validation

**Status:** Proposed (2026-07-18)
**Depends on:** SPEC-0003 (shared models, model visitor), SPEC-0005 (weight scoping rule, §2.5)
**Delivers:** SPEC-0005's open question "pre-render `weight` placement validation" — and the model visitor's first real server-side job

## Scope

A dependency-free validation pass in `shared/commonMain` that walks a parsed
model tree and reports **semantic** errors a well-formed payload can still
contain — starting with the one SPEC-0005 §2.5 explicitly deferred: a
`weight` modifier on a node whose parent provides no weight scope. Parsing
stays parsing (`require` in `init` covers everything a node can know about
*itself*); this pass covers what a node can only know about its *placement*.

This is exactly the job `KomposerModelVisitor` was promoted out of
`NiceToHave.kt` for (SPEC-0003 §2: "validation passes … a visitor that works
where Compose doesn't exist"). A backend can now reject a misassembled screen
before serving it; a client can front-run the render-time crash with a
structured error list.

## Non-goals

- **Removing the render-time backstop.** The fold's
  `KomposerRenderException` on a scope-less `weight` (SPEC-0005 §5.2) stays.
  Validation is *optional and advisory*; rendering remains defensive.
- **Wiring validation into the serializer.** `parse()` does not call
  `validate()` — a semantically invalid document is still a parseable one,
  and layering them keeps both APIs honest (and lets tools inspect invalid
  trees).
- Wire-format changes of any kind. Purely additive shared API.
- Schema/structural validation — unknown types, bad values, missing fields
  are parse-time failures already (SPEC-0001 §5).
- Traversing children of *unknown* composite node types (Open questions —
  needs a children-exposing abstraction that doesn't exist yet).

---

## 1. API (`shared/commonMain/…/core/validation/`)

```kotlin
// KomposerValidationError.kt
data class KomposerValidationError(
    /** Dot/bracket path to the offending element, e.g. "root.children[2].modifiers[0]". */
    val path: String,
    val message: String,
)

// KomposerValidationException.kt
class KomposerValidationException(
    val errors: List<KomposerValidationError>,
) : Exception("Invalid Komposer document: ${errors.size} error(s); first: ${errors.first().path} — ${errors.first().message}") {
    init { require(errors.isNotEmpty()) { "KomposerValidationException requires at least one error" } }
}

// KomposerValidator.kt
interface KomposerValidator {
    /** Collects every violation; never throws. Empty list ⇔ valid. */
    fun validate(document: KomposerDocument): List<KomposerValidationError>
    fun validate(root: KomposerModel): List<KomposerValidationError>
}

fun KomposerValidator.validateOrThrow(document: KomposerDocument) {
    val errors = validate(document)
    if (errors.isNotEmpty()) throw KomposerValidationException(errors)
}

// DefaultKomposerValidator.kt
class DefaultKomposerValidator : KomposerValidator { /* §3 */ }
```

Decisions, with rationale:

- **Collect-all, never throw from `validate`.** A server validating an
  authored screen wants *every* violation in one pass, not a fix-one-refetch
  loop. The throwing form is a thin extension for callers that just want a
  gate. (Contrast with parsing, which fails fast because a broken byte stream
  has no meaningful "rest of the errors".)
- **`KomposerValidationException` is not `KomposerParseException`.** The
  payload *parsed fine*; it is semantically misassembled. One exception type
  per failure class, same reasoning that split `KomposerRenderException` from
  `KomposerParseException` (SPEC-0004 §2).
- **Interface + default implementation** — the `KomposerSerializer` /
  `DefaultKomposerSerializer` pattern (SPEC-0003 §5), for the same reason:
  hosts can wrap/extend (e.g. add app-specific rules) behind the same seam.
- **Path syntax:** `root` for the document root, `.children[i]` per
  composite step, `.modifiers[j]` for the offending modifier. Stable,
  greppable, and mechanical to produce; not a formal JSON Pointer (Open
  questions).

## 2. Rule catalog v1

One rule. The catalog exists so the *next* placement rule lands as a row
here, not as a new API.

| # | Rule | Error message |
| --- | --- | --- |
| V1 | A `WeightModifier` may appear only in the `modifiers` list of a node whose **parent** node provides a weight scope. Scope providers in the current catalog: `column` (and `row` when SPEC-0006 lands). The document root has no parent and therefore never admits `weight`. | `"weight modifier requires a parent that provides a weight scope (column or row); <node> is at the document root"` / `"…; parent <parent> provides none"` |

This is the exact mirror of the render-time rule (SPEC-0005 §2.5/§5.3): the
fold throws where `scope == null`; V1 flags the same positions statically.

**Honesty note about today's catalog:** `column` is currently the only
composite, so every non-root node has a `column` parent and the only
*reachable* V1 violation is `weight` on the root node itself. The rule is
still written in terms of scope providers because the catalog's future makes
it real: SPEC-0006's `row` provides a scope, but `Box` (SPEC-0006 open
questions) will be the first composite that does **not** — the moment it
lands, mid-tree violations become expressible and this pass catches them
with no API change. Stating the invariant once, now, is the point.

## 3. Implementation: the visitor earns its keep

`DefaultKomposerValidator` walks the tree through `KomposerModelVisitor` — the
promised first real job. The key structural fact making this work: `accept`
does **not** auto-recurse (each model's `accept` is exactly
`visitor.visit(this)`), so the visitor controls traversal and can carry
parent context across it:

```kotlin
class DefaultKomposerValidator : KomposerValidator {

    override fun validate(document: KomposerDocument) = validate(document.root)

    override fun validate(root: KomposerModel): List<KomposerValidationError> =
        Walker().also { root.accept(it) }.errors

    private class Walker : KomposerModelVisitor {
        val errors = mutableListOf<KomposerValidationError>()
        private var path = "root"
        private var parentProvidesWeightScope = false   // root has no parent

        override fun visit(textModel: TextModel) = checkWeight(textModel)
        override fun visit(spacerModel: SpacerModel) = checkWeight(spacerModel)

        override fun visit(columnModel: ColumnModel) {
            checkWeight(columnModel)
            visitChildren(columnModel.children, providesWeightScope = true)
        }
        // SPEC-0006's RowModel: identical to ColumnModel (providesWeightScope = true).
        // The first scope-less composite (Box) will pass `false` — that is the whole rule.

        private fun visitChildren(children: List<KomposerModel>, providesWeightScope: Boolean) {
            val parentPath = path
            val outerScope = parentProvidesWeightScope
            parentProvidesWeightScope = providesWeightScope
            children.forEachIndexed { i, child ->
                path = "$parentPath.children[$i]"
                child.accept(this)
            }
            path = parentPath
            parentProvidesWeightScope = outerScope
        }

        private fun checkWeight(model: KomposerModel) {
            if (parentProvidesWeightScope) return
            model.modifiers.forEachIndexed { j, m ->
                if (m is WeightModifier) errors +=
                    KomposerValidationError(path = "$path.modifiers[$j]", message = /* §2 V1 */)
            }
        }
    }
}
```

- Pure Kotlin, zero new dependencies; runs on every KMP target and
  server-side — the pass a backend calls before serving a screen.
- Interplay with SPEC-0007: if the generic `visit(model: KomposerModel)`
  fallback lands, `Walker` inherits a sane degradation for *unknown* node
  types — their own `modifiers` are still checkable via the fallback, but
  their children (if any) are unreachable without a children-exposing
  abstraction; see Open questions. Neither spec depends on the other.

## 4. Tests (`commonTest`, all KMP targets)

| Test | Asserts |
| --- | --- |
| Root weight | `weight` in the root node's `modifiers` → exactly one error; `path == "root.modifiers[0]"` |
| Two root weights | both flagged: `root.modifiers[0]` and `root.modifiers[2]` (with a non-weight modifier between — index fidelity) |
| Column child | `weight` on a direct child of `column` → empty list |
| Deep nesting | `column > column > text(weight)` → empty; path bookkeeping restores correctly after the inner column (a *sibling* after it still reports the right path) |
| Whole-catalog sweep | the SPEC-0005 §9 reference payload validates clean |
| Document vs root overloads | `validate(document) == validate(document.root)` |
| `validateOrThrow` | valid → returns; invalid → `KomposerValidationException` with the same error list, message naming count and first path |
| Backstop unchanged | (androidApp, regression) the fold still throws `KomposerRenderException` for `scope = null` — validation being optional means the backstop must stay |

## Migration notes

None breaking — purely additive:

1. New package `core/validation/` with the four files of §1.
2. No serializer, model, renderer, or demo changes. (`MainActivity` *may*
   log `validate()` output before rendering as a living example; optional,
   not part of acceptance.)
3. SPEC-0006, if implemented after this spec, adds the `RowModel` overload to
   `Walker` with `providesWeightScope = true` (one line, noted in its §4
   checklist as visitor touch-point #5).
4. `specs/README.md` index gains this spec's row; SPEC-0005's open-question
   entry for pre-render validation is answered by this spec (recorded here;
   0005 is not edited — supersession flows forward).

## Acceptance criteria

- [ ] All §4 `commonTest` rows pass via `:shared:testDebugUnitTest` (and
      `:shared:allTests` on macOS).
- [ ] `grep -rE 'import (androidx|android|java)\.' shared/src/commonMain/`
      returns nothing.
- [ ] Every pre-existing test passes unchanged (`androidApp` is untouched
      except the backstop regression test).
- [ ] A README/CLAUDE.md line records that the model visitor now has a real
      consumer (the "first real job" promise is kept in the docs that made
      it).

## Open questions (deliberately deferred)

- **Children of unknown composites.** The walker can only recurse through
  composites it knows (`column`, then `row`). A `KomposerContainerModel`
  interface (`val children: List<KomposerModel>`) would generalize traversal —
  but it's a public model-API change deserving its own decision, and with
  SPEC-0007 it overlaps with per-registration metadata
  (`providesWeightScope`) as the way new composites declare their scope
  behavior. Decide when the first out-of-catalog composite actually appears.
- **Formal path syntax.** If tooling ever consumes errors mechanically,
  upgrade the path to RFC 6901 JSON Pointer (`/root/children/2/modifiers/0`);
  today's dot/bracket form is for humans and tests.
- **Rule growth.** Candidates when their features land: `clickable` without
  a registered action (Phase 5), theme-token references that the client
  can't resolve (theming), `spacing`-vs-arrangement conflicts are already
  parse-time (SPEC-0006 §7) and stay there.
- **Severity levels.** Everything is an error today; a `warning` tier (e.g.
  "weight will be ignored by pre-0006 clients") only earns its complexity
  once a consumer wants non-fatal findings.
