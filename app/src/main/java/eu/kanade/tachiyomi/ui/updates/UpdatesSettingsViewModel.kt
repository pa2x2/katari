package eu.kanade.tachiyomi.ui.updates

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.app.di.AppUiScope
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.updates.service.UpdatesPreferences

@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class UpdatesSettingsViewModel(
    val updatesPreferences: UpdatesPreferences,
    val getCategories: GetCategories,
) : ViewModel() {

    fun cycleCategory(category: Category) {
        cycle(category.id, updatesPreferences.filterIncludedCategories, updatesPreferences.filterExcludedCategories)
    }

    fun cycleSource(sourceId: Long) {
        cycle(sourceId, updatesPreferences.filterIncludedSources, updatesPreferences.filterExcludedSources)
    }

    fun cycleEntryType(type: EntryType) {
        val included = updatesPreferences.filterIncludedEntryTypes
        val excluded = updatesPreferences.filterExcludedEntryTypes
        when (type) {
            in excluded.get() -> excluded.getAndSet { it - type }
            in included.get() -> {
                included.getAndSet { it - type }
                excluded.getAndSet { it + type }
            }
            else -> included.getAndSet { it + type }
        }
    }

    fun toggleFilter(preference: (UpdatesPreferences) -> Preference<TriState>) {
        preference(updatesPreferences).getAndSet {
            it.next()
        }
    }

    fun clearFilters() {
        updatesPreferences.clearFeedFilter()
    }

    /** Moves a value from neither list to included, then to excluded, then back, as its tri-state box shows. */
    private fun cycle(value: Long, included: Preference<List<Long>>, excluded: Preference<List<Long>>) {
        when (value) {
            in excluded.get() -> excluded.getAndSet { it - value }
            in included.get() -> {
                included.getAndSet { it - value }
                excluded.getAndSet { it + value }
            }
            else -> included.getAndSet { it + value }
        }
    }
}
