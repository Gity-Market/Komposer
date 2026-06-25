package ir.gity.komposer.core.widget.factory


import androidx.compose.ui.unit.Density
import ir.gity.komposer.core.KomposerWidget
import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.model.spacer.SpacerModel
import ir.gity.komposer.core.model.text.TextModel
import ir.gity.komposer.core.widget.spacer.SpacerWidget
import ir.gity.komposer.core.widget.text.TextWidget

class TextWidgetFactory : KomposerWidgetFactory {
    override fun create(model: KomposerModel): KomposerWidget {
        require(model is TextModel)
        return TextWidget(
            text = model.text.orEmpty()
        )
    }
}

class SpacerWidgetFactory (
    private val density: Density // Inject it using dependency injection
) : KomposerWidgetFactory {
    override fun create(model: KomposerModel): KomposerWidget {
        require(model is SpacerModel)
        return SpacerWidget(
            pxDp = density.run { model.px.toDp() }
        )
    }
}