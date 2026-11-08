package eu.kanade.tachiyomi.ui.browse.extension

import app.katari.domain.extension.interactor.GetExtensionStores
import dev.zacsweers.metro.Inject
import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import tachiyomi.core.common.preference.getAndSet

@Inject
class ExtensionStoreFilterController(
    private val preferences: SourcePreferences,
    private val getExtensionStores: GetExtensionStores,
) {
    fun changes(): Flow<ExtensionStoreFilter> {
        return combine(
            getExtensionStores.subscribe(),
            preferences.disabledExtensionStores.changes(),
            ::ExtensionStoreFilter,
        ).distinctUntilChanged()
    }

    fun showAll() {
        preferences.disabledExtensionStores.set(emptySet())
    }

    fun toggle(storeUrl: String) {
        preferences.disabledExtensionStores.getAndSet { disabledStoreUrls ->
            if (storeUrl in disabledStoreUrls) {
                disabledStoreUrls - storeUrl
            } else {
                disabledStoreUrls + storeUrl
            }
        }
    }
}
