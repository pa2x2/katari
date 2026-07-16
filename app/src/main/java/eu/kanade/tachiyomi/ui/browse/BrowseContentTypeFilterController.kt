package eu.kanade.tachiyomi.ui.browse

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import tachiyomi.core.common.preference.getAndSet
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class BrowseContentTypeFilterController(
    private val preferences: SourcePreferences = Injekt.get(),
) {
    fun changes(): Flow<ContentTypeFilter> {
        return preferences.browseContentTypeFilter.changes()
            .map(ContentTypeFilter::fromPreferenceValue)
            .distinctUntilChanged()
    }

    fun showAll() {
        preferences.browseContentTypeFilter.set(emptySet())
    }

    fun toggle(entryType: EntryType) {
        update { current ->
            current.copy(
                entryTypes = if (entryType in current.entryTypes) {
                    current.entryTypes - entryType
                } else {
                    current.entryTypes + entryType
                },
            )
        }
    }

    fun toggleUnspecified() {
        update { current ->
            current.copy(includeUnspecified = !current.includeUnspecified)
        }
    }

    private fun update(transform: (ContentTypeFilter) -> ContentTypeFilter) {
        preferences.browseContentTypeFilter.getAndSet { value ->
            transform(ContentTypeFilter.fromPreferenceValue(value)).toPreferenceValue()
        }
    }
}
