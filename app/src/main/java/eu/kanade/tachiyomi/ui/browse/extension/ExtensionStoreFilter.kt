package eu.kanade.tachiyomi.ui.browse.extension

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.extension.model.Extension
import mihon.domain.extension.model.ExtensionStore

@Immutable
data class ExtensionStoreFilter(
    val stores: List<ExtensionStore> = emptyList(),
    val disabledStoreUrls: Set<String> = emptySet(),
) {
    val enabledStoreCount: Int
        get() = stores.count(::isEnabled)

    val isActive: Boolean
        get() = stores.isNotEmpty() && enabledStoreCount < stores.size

    fun isEnabled(store: ExtensionStore): Boolean {
        return store.indexUrl !in disabledStoreUrls
    }

    fun matches(extension: Extension): Boolean {
        if (!isActive) return true

        val store = when (extension) {
            is Extension.Available -> extension.store
            is Extension.Installed -> extension.store
            is Extension.Untrusted -> null
        }
        return store != null && isEnabled(store)
    }
}
