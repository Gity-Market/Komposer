package ir.gity.komposer.core.model

/**
 * The wire color format (SPEC-0001 §3): `#RRGGBB` or `#AARRGGBB`. Promoted out of
 * `TextModel.Companion` (SPEC-0005 §4) because more than one model now validates a color
 * (`text.color`, `background.color`) and a wire-format constant doesn't belong to one node.
 */
object WireColor {
    val REGEX = Regex("^#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")
}
