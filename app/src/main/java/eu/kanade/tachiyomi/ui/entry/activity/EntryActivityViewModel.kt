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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import logcat.LogPriority
import mihon.app.di.AppUiScope
import mihon.entry.interactions.statistics.EntryStatisticsFeature
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.statistics.entry.EntryActivityRepository
import tachiyomi.domain.statistics.entry.EntryActivitySummary
import tachiyomi.domain.statistics.entry.EntryCatchUpEstimate
import tachiyomi.domain.statistics.entry.EntryChapterDuration
import tachiyomi.domain.statistics.entry.EntryChapterRuntime
import java.time.LocalDate

/**
 * What the activity ledger knows about one title: totals for its activity line and sheet, the time each read chapter
 * took, and how long its unread chapters are estimated to take.
 *
 * @param memberIds the merged title's members, or the entry alone.
 */
@AssistedInject
class EntryActivityViewModel(
    @Assisted type: EntryType,
    @Assisted memberIds: List<Long>,
    repository: EntryActivityRepository,
    progressRepository: EntryProgressRepository,
    statisticsFeature: EntryStatisticsFeature,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppUiScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(type: EntryType, memberIds: List<Long>): EntryActivityViewModel
    }

    val state: StateFlow<EntryActivityViewModel.State>
        field = MutableStateFlow<EntryActivityViewModel.State>(State())

    private val contribution = statisticsFeature.contribution(type)

    /** The type's "Chapters read" wording; null when Statistics doesn't cover the type. */
    val consumedUnitLabel = contribution?.consumedUnitLabel

    init {
        val monthsSince = LocalDate.now().withDayOfMonth(1).minusMonths(MONTHS - 1L).toString()
        viewModelScope.launchIO {
            combine(
                repository.subscribeSummary(memberIds, monthsSince),
                repository.subscribeChapterDurations(memberIds),
                subscribeChapterRuntimes(progressRepository, memberIds),
            ) { summary, durations, runtimes ->
                State(
                    summary = summary.takeIf { it.totalDurationMillis > 0L },
                    readChapterDurations = durations
                        .filter(EntryChapterDuration::read)
                        .associate { it.chapterId to it.durationMillis },
                    catchUp = EntryCatchUpEstimate.of(runtimes, durations),
                )
            }
                .catch { logcat(LogPriority.ERROR, it) }
                .collectLatest { state.value = it }
        }
    }

    private fun subscribeChapterRuntimes(
        progressRepository: EntryProgressRepository,
        memberIds: List<Long>,
    ): Flow<List<EntryChapterRuntime>> {
        val reader = contribution?.chapterRuntime ?: return flowOf(emptyList())
        return combine(memberIds.map(progressRepository::getByEntryIdAsFlow)) { members ->
            // Nothing keeps a chapter to one saved state; the one played last says the most about it
            members.asList().flatten()
                .sortedBy(EntryProgressState::locatorUpdatedAt)
                .mapNotNull(reader::read)
                .associateBy(EntryChapterRuntime::chapterId)
                .values
                .toList()
        }
    }

    @Immutable
    data class State(
        /** Null when nothing was ever recorded for the title. */
        val summary: EntryActivitySummary? = null,
        val readChapterDurations: Map<Long, Long> = emptyMap(),
        val catchUp: EntryCatchUpEstimate = EntryCatchUpEstimate.Unknown,
    )

    companion object {
        /** Months shown in the sheet, ending with the current one. */
        const val MONTHS = 12
    }
}
