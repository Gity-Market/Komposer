package ir.gity.komposer.core.model.text

import androidx.compose.runtime.Composable
import ir.gity.komposer.core.KomposerWidget
import ir.gity.komposer.core.base.KomposerModelVisitor
import ir.gity.komposer.core.model.KomposerModel
import ir.gity.komposer.core.widget.text.TextWidget
import kotlinx.serialization.Serializable

@Serializable
data class TextModel(
    val text: String? = null
) : KomposerModel {
    override fun toWidget(): KomposerWidget {
        return TextWidget(
            text = text.orEmpty()
        )
    }
    override fun accept(visitor: KomposerModelVisitor) {
        visitor.visit(this)
    }
}

