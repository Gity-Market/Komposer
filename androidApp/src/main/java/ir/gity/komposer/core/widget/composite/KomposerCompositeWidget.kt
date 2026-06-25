package ir.gity.komposer.core.widget.composite

import ir.gity.komposer.core.KomposerWidget

interface KomposerCompositeWidget : KomposerWidget {
    fun addChild(widget: KomposerWidget)
    fun removeChild(widget: KomposerWidget)
    fun getChildren(): List<KomposerWidget>
}