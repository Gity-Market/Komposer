package ir.gity.komposer.core.model

/**
 * The wire color format: `#RRGGBB` or `#AARRGGBB`. Promoted out of
 * `TextModel.Companion` because more than one model now validates a color
 * (`text.color`, `background.color`) and a wire-format constant doesn't belong to one node.
 */
object WireColor {
    val REGEX = Regex("^#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")
}
