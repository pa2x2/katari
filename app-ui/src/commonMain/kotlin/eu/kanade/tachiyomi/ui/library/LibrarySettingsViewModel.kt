package eu.kanade.tachiyomi.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.ui.di.AppUiScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.category.interactor.SetDisplayMode
import tachiyomi.domain.category.interactor.SetSortModeForCategory
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryGrouping
import tachiyomi.domain.library.model.LibraryPinnedDisplayStyle
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.service.LibraryPreferences

@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class LibrarySettingsViewModel(
    val libraryPreferences: LibraryPreferences,
    private val setDisplayMode: SetDisplayMode,
    private val setSortModeForCategory: SetSortModeForCategory,
    val releasePeriodFilter: ReleasePeriodFilterAvailability,
) : ViewModel() {

    fun toggleFilter(preference: (LibraryPreferences) -> Preference<TriState>) {
        preference(libraryPreferences).getAndSet {
            it.next()
        }
    }

    fun setDisplayMode(mode: LibraryDisplayMode) {
        setDisplayMode.await(mode)
    }

    fun setPinnedDisplayStyle(style: LibraryPinnedDisplayStyle) {
        libraryPreferences.pinnedDisplayStyle.set(style)
    }

    fun setSort(category: Category?, mode: LibrarySort.Type, direction: LibrarySort.Direction) {
        viewModelScope.launchIO {
            val targetCategory = category
                ?.takeIf { !it.isSystemCategory }
            setSortModeForCategory.await(targetCategory, mode, direction)
        }
    }

    fun setGrouping(grouping: LibraryGrouping) {
        libraryPreferences.grouping.set(grouping)
    }
}
