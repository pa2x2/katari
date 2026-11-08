package eu.kanade.tachiyomi.ui.stats.recap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.entry.interactions.statistics.EntryStatisticsFeature
import app.katari.profiles.core.ProfileStore
import app.katari.ui.di.AppUiScope
import coil3.PlatformContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import eu.kanade.tachiyomi.ui.stats.recap.cover.recapCoverSeedColor
import eu.kanade.tachiyomi.ui.stats.recap.delivery.editionKey
import eu.kanade.tachiyomi.ui.stats.recap.delivery.newYearRecap
import eu.kanade.tachiyomi.ui.stats.recap.period.StatisticsRecapPeriod
import eu.kanade.tachiyomi.ui.stats.recap.period.previous
import eu.kanade.tachiyomi.ui.stats.recap.period.scope
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapStory
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapTitle
import eu.kanade.tachiyomi.ui.stats.recap.story.buildStatisticsRecapStory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.source.service.NsfwSourceIds
import tachiyomi.domain.statistics.recap.StatisticsRecapActivity
import tachiyomi.domain.statistics.recap.StatisticsRecapPeriodTotals
import tachiyomi.domain.statistics.recap.StatisticsRecapRepository
import tachiyomi.domain.statistics.service.StatisticsPreferences
import java.time.LocalDate

/**
 * Loads a period's activity once, then rebuilds its story whenever titles are hidden or shown, so hiding a title
 * takes effect on the page being watched.
 */
@AssistedInject
class StatisticsRecapViewModel(
    @Assisted private val period: StatisticsRecapPeriod,
    private val imageContext: PlatformContext,
    activeProfileProvider: ActiveProfileProvider,
    profileStore: ProfileStore,
    private val recapRepository: StatisticsRecapRepository,
    private val statisticsFeature: EntryStatisticsFeature,
    private val nsfwSourceIds: NsfwSourceIds,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppUiScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(period: StatisticsRecapPeriod): StatisticsRecapViewModel
    }

    val state: StateFlow<StatisticsRecapViewModel.State>
        field = MutableStateFlow<StatisticsRecapViewModel.State>(State.Loading)

    private val profileId = activeProfileProvider.activeProfileId
    private val preferences = StatisticsPreferences(profileStore.profileStore(profileId))

    /** Hidden titles are kept per year, so a title hidden from a month stays hidden from that year's other recaps. */
    private val hiddenPrefix = "${period.end.year}:"

    private val mutableCoverSeeds = MutableStateFlow<Map<Long, Int?>>(emptyMap())

    /** The colour each featured cover was reduced to; null where a cover has no usable colour. */
    val coverSeeds: StateFlow<Map<Long, Int?>> = mutableCoverSeeds.asStateFlow()

    init {
        if (period is StatisticsRecapPeriod.Year && period.editionKey == newYearRecap(LocalDate.now())?.editionKey) {
            preferences.lastOpenedYearRecap.set(period.editionKey)
        }
        viewModelScope.launchIO {
            val loaded = try {
                load()
            } catch (error: Exception) {
                logcat(LogPriority.ERROR, error)
                null
            }
            when {
                loaded == null -> state.update { State.Failed }
                loaded.first.segments.isEmpty() -> state.update { State.Empty }
                else -> collectStories(loaded.first, loaded.second)
            }
        }
    }

    private suspend fun load() = withIOContext {
        val activity = period.scope(
            recapRepository.getActivity(profileId, period.start.toString(), period.end.toString()),
        )
        val previous = period.previous()?.let { previous ->
            previous to recapRepository.getPeriodTotals(profileId, previous.start.toString(), previous.end.toString())
        }
        activity to previous
    }

    private suspend fun collectStories(
        activity: StatisticsRecapActivity,
        previous: Pair<StatisticsRecapPeriod, StatisticsRecapPeriodTotals>?,
    ) {
        combine(
            preferences.recapHiddenEntries.changes(),
            preferences.recapIncludeNsfw.changes(),
            nsfwSourceIds.subscribe(),
        ) { hiddenKeys, includeNsfw, nsfw ->
            val handHidden = hiddenKeys.filter { it.startsWith(hiddenPrefix) }
                .mapNotNullTo(HashSet()) { it.removePrefix(hiddenPrefix).toLongOrNull() }
            val nsfwHidden = if (includeNsfw) {
                emptySet()
            } else {
                activity.entries.filter {
                    it.sourceId in nsfw
                }.map { it.id }
            }
            val story = buildStatisticsRecapStory(
                period = period,
                activity = activity,
                previous = previous,
                hiddenEntryIds = handHidden + nsfwHidden,
                contributions = statisticsFeature.contributions,
            )
            val hiddenTitles = activity.entries.filter { it.id in handHidden }
                .map { StatisticsRecapTitle(it.id, it.type, it.title, it.cover, 0L) }
            State.Success(story, hiddenTitles, includeNsfw)
        }.collect { success ->
            state.update { success }
            loadCoverSeeds(success.story)
        }
    }

    private suspend fun loadCoverSeeds(story: StatisticsRecapStory) {
        story.pages.mapNotNull { it.featured }.distinctBy { it.entryId }
            .filterNot { it.entryId in mutableCoverSeeds.value }
            .forEach { title ->
                val seed = try {
                    imageContext.recapCoverSeedColor(title.cover)
                } catch (error: Exception) {
                    logcat(LogPriority.WARN, error)
                    null
                }
                mutableCoverSeeds.update { it + (title.entryId to seed) }
            }
    }

    fun hide(entryId: Long) {
        preferences.recapHiddenEntries.set(preferences.recapHiddenEntries.get() + "$hiddenPrefix$entryId")
    }

    fun showAgain(entryId: Long) {
        preferences.recapHiddenEntries.set(preferences.recapHiddenEntries.get() - "$hiddenPrefix$entryId")
    }

    fun setIncludeNsfw(include: Boolean) {
        preferences.recapIncludeNsfw.set(include)
    }

    sealed interface State {
        data object Loading : State

        data object Failed : State

        /** Nothing was timed in the period. */
        data object Empty : State

        /** @param hiddenTitles titles hidden by hand; titles left out as 18+ aren't listed. */
        data class Success(
            val story: StatisticsRecapStory,
            val hiddenTitles: List<StatisticsRecapTitle>,
            val includeNsfw: Boolean,
        ) : State
    }
}
