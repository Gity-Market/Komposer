package ir.gity.komposer.core.model

// commonMain — no Compose, no widget, no java.* imports allowed in this layer.
interface KomposerModel {
    fun accept(visitor: KomposerModelVisitor)
}
