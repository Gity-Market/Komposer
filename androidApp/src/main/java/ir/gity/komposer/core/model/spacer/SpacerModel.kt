package ir.gity.komposer.core.model.spacer

import androidx.compose.ui.unit.dp
import ir.gity.komposer.core.KomposerWidget
import ir.gity.komposer.core.base.KomposerModelVisitor
import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.widget.spacer.SpacerWidget
import kotlinx.serialization.Serializable

@Serializable
data class SpacerModel(
    val px: Float
) : KomposerModel {
    override fun toWidget(): KomposerWidget {
        return SpacerWidget(16.dp)
    }
    override fun accept(visitor: KomposerModelVisitor) {
        visitor.visit(this)
    }
}