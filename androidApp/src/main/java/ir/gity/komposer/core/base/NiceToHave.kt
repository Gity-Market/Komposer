package ir.gity.komposer.core.base

import androidx.compose.ui.text.TextStyle
import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.text.TextModel
import ir.gity.komposer.core.widget.text.TextWidget

// Scratch file for design sketches that are not yet real API. The serializer, mappers,
// engine, JSON factory, model visitor, and factory registry have graduated out of here
// (SPEC-0003/0004); what remains is parked until it becomes real.

interface KomposerState {
    fun saveState(): KomposerModel
    fun restoreState(state: KomposerModel)
}

interface Specification<T> {
    fun isSatisfiedBy(candidate: T): Boolean
}

class NonEmptyTextSpecification : Specification<TextModel> {
    override fun isSatisfiedBy(candidate: TextModel): Boolean {
        return candidate.text.isNotBlank()
    }
}

fun createTextWidget(model: TextModel): TextWidget {
    val spec = NonEmptyTextSpecification()
    if (!spec.isSatisfiedBy(model)) {
        throw IllegalArgumentException("Text cannot be empty!")
    }
    return TextWidget(
        text = model.text,
        style = TextStyle.Default,
    )
}
