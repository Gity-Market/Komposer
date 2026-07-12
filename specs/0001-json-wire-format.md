# SPEC-0001 — JSON Wire Format v1

**Status:** Implemented (2026-07-12)
**Depends on:** —
**Implemented by:** SPEC-0003 (serializer), SPEC-0002 (node fields)

## Scope

The shape of the JSON that travels between server and client: the document
envelope, how node types are discriminated, the conventions for scalar values,
and how strict the client parser is. This spec deliberately says nothing about
*which* fields each node has — that's SPEC-0002.

## Non-goals

- Modifiers (roadmap Phase 3).
- Actions/events (roadmap Phase 5).
- Version *negotiation* or graceful degradation for unknown nodes (later phase;
  v1 policy below is intentionally strict).

---

## 1. Document envelope

Every payload is a **document**: a JSON object with exactly two required fields.

```json
{
  "version": 1,
  "root": { "type": "column", "children": [] }
}
```

| Field | Type | Required | Meaning |
| --- | --- | --- | --- |
| `version` | integer | yes | Wire-format version. Must be `1`. |
| `root` | node object | yes | The root of the UI tree. Any node type is legal as root. |

**Rules:**

- A document with `version != 1` fails parsing with `KomposerParseException`
  (`"Unsupported wire version: N"`). Forward-compat policy is deferred until a
  version 2 exists; being strict now is what makes leniency possible later.
- `version` is always written on encode, never omitted.
- The `"children": []` in the example is illustrative — an empty column may
  also omit `children` entirely; both parse identically, and the canonical
  *encoding* omits it (`encodeDefaults = false`, §4).

Rationale: an envelope from day one costs one nesting level and buys us schema
evolution, per-screen metadata (title, cache hints), and multi-root documents
later without a breaking change.

## 2. Nodes and the `type` discriminator

Every node is a JSON object with a required `"type"` string field. v1 types:

| `type` | Model class | Spec |
| --- | --- | --- |
| `"text"` | `TextModel` | SPEC-0002 §1 |
| `"column"` | `ColumnModel` | SPEC-0002 §2 |
| `"spacer"` | `SpacerModel` | SPEC-0002 §3 |

**Rules:**

- Wire names are bound with `@SerialName("text")` etc. on the model classes, so
  Kotlin class renames never break the wire format.
- The discriminator key is `"type"`, configured explicitly
  (`classDiscriminator = "type"`), not left to library defaults.
- A node whose `type` is missing or unregistered fails parsing with
  `KomposerParseException`. (The current demo JSON in `MainActivity.kt` has no
  `type` fields — it was never parseable and must be updated; see SPEC-0004.)
- `type` is **reserved**: no node may declare a wire field named `type`. It
  collides with the class discriminator, and kotlinx.serialization rejects such
  a class at encode time.

## 3. Scalar conventions

The server cannot know device density, so **no pixel values ever appear on the
wire**. All conventions below apply uniformly to every node, present and future.

| Kind | Wire type | Interpretation | Example |
| --- | --- | --- | --- |
| Layout dimension | number | **dp** | `"height": 16` |
| Font size / letter spacing / line height | number | **sp** | `"fontSize": 14` |
| Color | string | `#RRGGBB` or `#AARRGGBB`, case-insensitive, `#` required | `"color": "#FF6200EE"` |
| Enum | string | lowerCamelCase token from a closed set | `"overflow": "ellipsis"` |
| Font weight | integer | `1..1000` (CSS-style; 400 normal, 700 bold) | `"fontWeight": 700` |
| Flag | boolean | — | `"softWrap": false` |

Color strings must match `^#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$`; six digits imply
alpha `FF`. Anything else fails parsing.

## 4. Optionality and defaults

- **Absent means "unspecified"**: the client applies its own default (usually
  the Compose default / theme value). Optional fields map to nullable Kotlin
  properties defaulting to `null`.
- **Encoding is minimal**: the serializer is configured with
  `encodeDefaults = false`, so unset fields are omitted, never written as
  `null`. `{"type": "text", "text": "hi"}` is the canonical encoding of a
  default text node.
- **Required fields** have no Kotlin default; a payload missing one fails
  parsing.

## 5. Strictness policy (v1)

| Situation | Behavior |
| --- | --- |
| Unknown `type` | **Fail** (`KomposerParseException`) |
| Unknown *field* on a known node | **Ignore** (`ignoreUnknownKeys = true`) |
| Explicit `null` for an optional field | **Accept** — treated as absent |
| Missing required field | **Fail** |
| Value out of range / bad format | **Fail** (validation in model `init`, see SPEC-0002 §4) |
| `version != 1` | **Fail** |

Ignoring unknown fields is the one forward-compat door we keep open in v1: a
newer server may add optional fields to existing nodes without breaking older
clients. Unknown *node types* stay fatal until we design real fallback
rendering (roadmap Phase 6) — silently dropping UI is worse than failing loudly
while the format is young.

## 6. Round-trip requirement

For every model `m` in the v1 catalog and every document `d`:

```
parseNode(encodeNode(m)) == m        // data-class equality
parse(encode(d)) == d
```

This is enforced by tests in SPEC-0003 and is the definition of "the JSON loop
is closed."

## 7. Reference payload

The v1 replacement for the sample in `KomposerJson2ModelDemo()`:

```json
{
  "version": 1,
  "root": {
    "type": "column",
    "children": [
      { "type": "text", "text": "Hello Komposer", "fontWeight": 700, "fontSize": 20, "color": "#6200EE" },
      { "type": "text", "text": "One line only, ellipsized when it overflows the width", "maxLines": 1, "overflow": "ellipsis" },
      { "type": "spacer", "height": 16 },
      {
        "type": "column",
        "children": [
          { "type": "text", "text": "Nested, italic", "fontStyle": "italic" }
        ]
      }
    ]
  }
}
```

## Acceptance criteria

- [ ] The reference payload above parses into the expected model tree (asserted
      field-by-field in a test).
- [ ] Round-trip equality holds for minimal and fully-populated variants of all
      three node types.
- [ ] Each strictness rule in §5 has a dedicated failing-input test.

## Open questions (deliberately deferred)

- Multi-document / multi-screen payloads and per-screen metadata in the envelope.
- A machine-readable schema (JSON Schema) generated from the models, for
  non-Kotlin backends.
