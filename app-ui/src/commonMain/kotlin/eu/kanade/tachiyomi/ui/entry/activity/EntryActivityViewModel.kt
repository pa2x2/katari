package eu.kanade.tachiyomi.ui.entry.activity

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import mihon.app.di.AppUiScope
import mihon.entry.interactions.statistics.EntryStatisticsFeature
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.statistics.entry.EntryActivityRepository
import tachiyomi.domain.statistics.entry.EntryActivitySummary
import tachiyomi.domain.statistics.entry.EntryCatchUpPace
import tachiyomi.domain.statistics.entry.EntryChapterDuration
import tachiyomi.domain.statistics.entry.EntryChapterPace
import java.time.LocalDate

/**
 * What the activity ledger knows about one title: totals for its activity line and sheet, the time each read chapter
 * took, and the pace its unread chapters are estimated at.
 *
 * @param memberIds the merged title's members, or the entry alone.
 */
@AssistedInject
class EntryActivityViewModel(
    @Assisted private val profileId: Long,
    @Assisted private val type: EntryType,
    @Assisted memberIds: List<Long>,
    private val repository: EntryActivityRepository,
    statisticsFeature: EntryStatisticsFeature,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppUiScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(profileId: Long, type: EntryType, memberIds: List<Long>): EntryActivityViewModel
    }

    val state: StateFlow<EntryActivityViewModel.State>
        field = MutableStateFlow<EntryActivityViewModel.State>(State())

    private val contribution = statisticsFeature.contribution(type)

    /** The type's "Chapters read" wording; null when Statistics doesn't cover the type. */
    val consumedUnitLabel = contribution?.consumedUnitLabel

    private var typePace: EntryChapterPace? = null
    private var typePaceLoaded = false

    init {
        val monthsSince = LocalDate.now().withDayOfMonth(1).minusMonths(MONTHS - 1L).toString()
        viewModelScope.launchIO {
            combine(
                repository.subscribeSummary(memberIds, monthsSince),
                repository.subscribeChapterDurations(memberIds),
            ) { summary, durations -> summary to durations }
                .catch { logcat(LogPriority.ERROR, it) }
                .collectLatest { (summary, durations) ->
                    val pace = EntryCatchUpPace.select(
                        title = EntryCatchUpPace.titlePace(durations),
                        type = loadTypePace(durations),
                        borrowTypePace = contribution?.itemPaceCarriesAcrossTitles == true,
                    )
                    state.update {
                        State(
                            summary = summary.takeIf { it.totalDurationMillis > 0L },
                            readChapterDurations = durations
                                .filter(EntryChapterDuration::read)
                                .associate { it.chapterId to it.durationMillis },
                            paceMillis = pace,
                        )
                    }
                }
        }
    }

    /** The type-wide median is only needed until the title has enough timed chapters of its own. */
    private suspend fun loadTypePace(durations: List<EntryChapterDuration>): EntryChapterPace? {
        if (contribution?.itemPaceCarriesAcrossTitles != true) return null
        if (durations.count(EntryChapterDuration::read) >= EntryCatchUpPace.MINIMUM_SAMPLES) return null
        if (!typePaceLoaded) {
            typePace = repository.getTypeChapterPace(profileId, type)
            typePaceLoaded = true
        }
        return typePace
    }

    @Immutable
    data class State(
        /** Null when nothing was ever recorded for the title. */
        val summary: EntryActivitySummary? = null,
        val readChapterDurations: Map<Long, Long> = emptyMap(),
        /** Expected time of one unread chapter; null when there's too little to go on. */
        val paceMillis: Long? = null,
    )

    companion object {
        /** Months shown in the sheet, ending with the current one. */
        const val MONTHS = 12
    }
}
