package ir.gity.komposer.core.model

import kotlinx.serialization.Serializable

/**
 * The wire envelope (SPEC-0001 §1). `version` has no default — it is required on the
 * wire and always encoded.
 *
 * The check is a plain `require`, not a `KomposerParseException`: the `model/` package
 * must not depend on `serialization/`, and a backend *constructing* an unsupported
 * document should get an ordinary `IllegalArgumentException`. During parsing the
 * serializer wraps it into `KomposerParseException` (SPEC-0003 §5), preserving the
 * "Unsupported wire version: N" message SPEC-0001 §1 promises.
 */
@Serializable
data class KomposerDocument(
    val version: Int,
    val root: KomposerModel,
) {
    init {
        require(version == 1) { "Unsupported wire version: $version" }
    }
}
