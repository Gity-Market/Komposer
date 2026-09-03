package ir.gity.komposer.core

/**
 * Mirrors `KomposerParseException` for the construction/render stage: a payload that parsed
 * cleanly but cannot be turned into pixels must fail loudly, not vanish.
 *
 * Two throw sites remain, and both are payload errors rather than programming errors — the
 * "unregistered factory" and "unhandled widget" cases are gone, dissolved by sealing the widget
 * hierarchy (a missing render branch is now a compile error, and there is no registry to miss):
 *
 * - a `weight` modifier folded with no weight-capable parent scope (`KomposerModifierFold`),
 * - a wire color that is not `#RRGGBB` / `#AARRGGBB` (`parseKomposerColor`).
 */
class KomposerRenderException(message: String) : Exception(message)
