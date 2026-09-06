package eu.kanade.tachiyomi.ui.browse.extension

import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import mihon.domain.extension.interactor.GetExtensionStores
import tachiyomi.core.common.preference.getAndSet
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ExtensionStoreFilterController(
    private val preferences: SourcePreferences = Injekt.get(),
    private val getExtensionStores: GetExtensionStores = Injekt.get(),
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
