package ir.gity.komposer.core.serialization

/**
 * One exception type for "the payload is bad" — a single catch point regardless of
 * whether kotlinx.serialization or a model `init` check rejected it (SPEC-0003 §5).
 */
class KomposerParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
