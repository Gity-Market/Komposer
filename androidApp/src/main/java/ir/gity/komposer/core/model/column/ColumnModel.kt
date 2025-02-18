package ir.gity.komposer.core.model.column

import androidx.compose.runtime.Composable
import ir.gity.komposer.core.KomposerWidget
import ir.gity.komposer.core.base.KomposerModelVisitor
import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.widget.column.ColumnWidget
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class ColumnModel(
    val children: List<@Contextual KomposerModel>
) : KomposerModel {
    override fun toWidget(): KomposerWidget {
        return ColumnWidget(
            children = children.map { it.toWidget() }.toMutableList(),
        )
    }

    override fun accept(visitor: KomposerModelVisitor) {
        visitor.visit(this)
    }
}
