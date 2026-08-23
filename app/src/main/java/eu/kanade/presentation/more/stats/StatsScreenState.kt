package eu.kanade.presentation.more.stats

import androidx.compose.runtime.Immutable
import eu.kanade.presentation.more.stats.data.StatsActivity
import eu.kanade.presentation.more.stats.data.StatsLibrary
import eu.kanade.presentation.more.stats.data.StatsRange
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType

sealed interface StatsScreenState {
    @Immutable
    data object Loading : StatsScreenState

    @Immutable
    data class Success(
        val profileId: Long,
        val range: StatsRange,
        val selectedType: EntryType?,
        val types: List<StatsType>,
        val library: StatsLibrary,
        val activity: ActivityState,
        val incognito: Boolean,
    ) : StatsScreenState
}

sealed interface ActivityState {
    @Immutable
    data object Loading : ActivityState

    @Immutable
    data class Available(val data: StatsActivity) : ActivityState

    @Immutable
    data object Failed : ActivityState
}
