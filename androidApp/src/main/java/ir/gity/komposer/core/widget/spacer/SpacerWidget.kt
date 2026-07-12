package ir.gity.komposer.core.widget.spacer

import androidx.compose.ui.unit.Dp
import ir.gity.komposer.core.KomposerWidget
import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.spacer.SpacerModel
import ir.gity.komposer.core.visitor.KomposerWidgetVisitor

// Concrete Element
data class SpacerWidget(
    val height: Dp,
) : KomposerWidget {
    // Faithful: the real height in dp (was a hardcoded 26f).
    override fun toModel(): KomposerModel = SpacerModel(height = height.value)

    override fun Accept(visitor: KomposerWidgetVisitor) {
        visitor.Visit(this)
    }
}
