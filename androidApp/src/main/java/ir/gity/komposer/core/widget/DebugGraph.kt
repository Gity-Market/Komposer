package ir.gity.komposer.core.widget

/**
 * Debug traversal — plain data, not UI, so it runs anywhere: tests, background
 * threads. Replaces the deleted widget visitor + graph
 * builder pair, whose own dispatch `when` was already evidence the pattern fought the language.
 */
fun KomposerWidget.debugGraph(): String = buildString { appendGraph(this@debugGraph) }

private fun StringBuilder.appendGraph(widget: KomposerWidget) {
    when (widget) {  // exhaustive — a new widget type must decide its debug line
        is TextWidget -> appendLine("Text Widget(text: ${widget.text})")
        is ColumnWidget -> {
            appendLine("Column(size: ${widget.children.size})")
            widget.children.forEach { appendGraph(it) }
        }
        is RowWidget -> {
            appendLine("Row(size: ${widget.children.size})")
            widget.children.forEach { appendGraph(it) }
        }
        is BoxWidget -> {
            appendLine("Box(size: ${widget.children.size})")
            widget.children.forEach { appendGraph(it) }
        }
        is SpacerWidget -> appendLine("Spacer(value: ${widget.height})")
    }
}
