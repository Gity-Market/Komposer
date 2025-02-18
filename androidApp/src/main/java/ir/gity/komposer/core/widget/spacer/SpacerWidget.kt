package ir.gity.komposer.core.widget.spacer

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import ir.gity.komposer.core.KomposerWidget
import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.spacer.SpacerModel
import ir.gity.komposer.core.visitor.KomposerWidgetVisitor

data class SpacerWidget(
    val pxDp: Dp
) : KomposerWidget {
    override fun toModel(): KomposerModel {
        return SpacerModel(
            px = 26f
        )
    }

    @Composable
    override fun Accept(visitor: KomposerWidgetVisitor) {
        visitor.Visit(this)
    }
}

