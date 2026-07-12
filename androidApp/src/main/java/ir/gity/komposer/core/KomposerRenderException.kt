package ir.gity.komposer.core

/**
 * Mirrors `KomposerParseException` for the construction/render stage (SPEC-0004 §2):
 * an unregistered factory or an unhandled widget is a programming error and must fail
 * loudly, not vanish.
 */
class KomposerRenderException(message: String) : Exception(message)
