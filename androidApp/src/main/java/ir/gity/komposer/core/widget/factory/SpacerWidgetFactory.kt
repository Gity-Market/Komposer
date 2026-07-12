package ir.gity.komposer.core.widget.factory

import androidx.compose.ui.unit.dp
import ir.gity.komposer.core.KomposerWidget
import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.spacer.SpacerModel
import ir.gity.komposer.core.widget.spacer.SpacerWidget

/**
 * dp on the wire means no `Density` conversion — `SpacerWidget(model.height.dp)`. Dropping
 * the `Density` constructor param unblocks registering factories outside a composition
 * (SPEC-0004 §3).
 */
class SpacerWidgetFactory : KomposerWidgetFactory {
    override fun create(model: KomposerModel, root: KomposerWidgetFactory): KomposerWidget {
        require(model is SpacerModel) { "SpacerWidgetFactory received ${model::class.simpleName}" }
        return SpacerWidget(height = model.height.dp)
    }
}
