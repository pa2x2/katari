package app.katari.data.extension.model

import app.katari.domain.extension.model.ExtensionStore

interface BaseNetworkExtensionStore {
    fun toExtensionStore(indexUrl: String): ExtensionStore
}
