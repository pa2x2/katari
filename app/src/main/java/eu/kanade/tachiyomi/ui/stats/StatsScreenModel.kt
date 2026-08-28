package eu.kanade.tachiyomi.ui.stats

import androidx.compose.ui.util.fastDistinctBy
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.more.stats.ActivityState
import eu.kanade.presentation.more.stats.StatsScreenState
import eu.kanade.presentation.more.stats.data.StatsActivityWindow
import eu.kanade.presentation.more.stats.data.StatsLibrary
import eu.kanade.presentation.more.stats.data.StatsRange
import eu.kanade.presentation.more.stats.data.StatsType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import mihon.entry.interactions.presentation.EntryTypePresentationFeature
import mihon.entry.interactions.statistics.EntryStatisticsFeature
import mihon.feature.profiles.core.ProfileScopedStateEvent
import mihon.feature.profiles.core.observeProfileScopedState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.entry.interactor.GetLibraryEntries
import tachiyomi.domain.statistics.repository.StatisticsRepository
import tachiyomi.domain.statistics.service.StatisticsPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Locale

class StatsScreenModel(
    private val activeProfileProvider: ActiveProfileProvider = Injekt.get(),
    private val getLibraryEntries: GetLibraryEntries = Injekt.get(),
    private val presentationFeature: EntryTypePresentationFeature = Injekt.get(),
    private val statisticsFeature: EntryStatisticsFeature = Injekt.get(),
    private val statisticsRepository: StatisticsRepository = Injekt.get(),
    private val statisticsPreferences: StatisticsPreferences = Injekt.get(),
    private val basePreferences: BasePreferences = Injekt.get(),
) : StateScreenModel<StatsScreenState>(StatsScreenState.Loading) {

    private val activityReload = MutableStateFlow(0L)
    private val today = MutableStateFlow(LocalDate.now())
    private val finiteWindowEndDate = MutableStateFlow<LocalDate?>(null)
    private val types = buildStatisticsTypes(statisticsFeature, presentationFeature)

    init {
        screenModelScope.launchIO {
            observeProfileScopedState(activeProfileProvider.activeProfileIdFlow) { profileId ->
                combine(
                    getLibraryEntries.subscribe(profileId).map { libraryEntries ->
                        val distinctEntries = libraryEntries.fastDistinctBy { it.key }
                        StatsLibrary(
                            totalTitles = distinctEntries.size,
                            titlesByType = distinctEntries.groupingBy { it.entry.type }.eachCount(),
                            progress = buildLibraryProgress(distinctEntries),
                            progressByType = distinctEntries.groupBy { it.entry.type }
                                .mapNotNull { (type, items) ->
                                    buildLibraryProgress(items)?.let { type to it }
                                }
                                .toMap(),
                            insightsByType = distinctEntries
                                .groupBy { it.entry.type }
                                .mapValues { (_, items) -> buildLibraryInsights(items) },
                        )
                    },
                    combine(
                        statisticsPreferences.selectedRange.changes(),
                        finiteWindowEndDate,
                        today,
                        activityReload,
                    ) { rangeName, historicalEndDate, today, reloadToken ->
                        val range = StatsRange.entries.find { it.name == rangeName } ?: StatsRange.THIRTY_DAYS
                        val endDate = when (range) {
                            StatsRange.ALL -> today
                            else -> historicalEndDate?.coerceAtMost(today) ?: today
                        }
                        StatisticsActivityLoadRequest(
                            window = range.windowEndingOn(
                                endDate = endDate,
                                isLatest = range == StatsRange.ALL || historicalEndDate == null,
                            ),
                            reloadToken = reloadToken,
                        )
                    }.distinctUntilChanged().flatMapLatest { request ->
                        val window = request.window
                        val navigationWindow = window.navigationWindow(today.value)
                        val loadEvents: Flow<StatisticsActivityLoadEvent> = combine(
                            statisticsRepository.subscribeActivity(
                                profileId = profileId,
                                startLocalDate = window.startDate?.toString(),
                                endLocalDate = window.endDate.toString(),
                            ),
                            statisticsRepository.subscribeActivityTimeline(
                                profileId = profileId,
                                startLocalDate = navigationWindow.startDate?.toString(),
                                endLocalDate = navigationWindow.endDate.toString(),
                            ),
                            statisticsRepository.subscribeActivityTimeline(
                                profileId = profileId,
                                startLocalDate = null,
                                endLocalDate = window.endDate.toString(),
                            ),
                        ) { snapshot, timeline, streakTimeline ->
                            StatisticsActivityLoadEvent.Loaded(
                                request = request,
                                data = buildWindowActivity(
                                    snapshot = snapshot,
                                    window = window,
                                    types = types.map(StatsType::type),
                                    locale = Locale.getDefault(),
                                    navigationTimeline = timeline,
                                    streakTimeline = streakTimeline,
                                    navigationStartDate = navigationWindow.startDate,
                                    navigationEndDate = navigationWindow.endDate,
                                ),
                            )
                        }
                        loadEvents.onStart {
                            emit(StatisticsActivityLoadEvent.Loading(request))
                        }.catch { error ->
                            logcat(LogPriority.ERROR, error)
                            emit(StatisticsActivityLoadEvent.Failed(request))
                        }
                    }.runningFold<StatisticsActivityLoadEvent, ActivityState?>(null, ::reduceStatisticsActivityRequest)
                        .filterNotNull()
                        .onEach(::restoreDisplayedActivitySelectionAfterFailure)
                        .map { activity -> activity.displayedRange to activity },
                    statisticsPreferences.selectedType.changes(),
                    basePreferences.incognitoMode.changes(),
                ) { library, (range, activity), selectedTypeName, incognito ->
                    StatsScreenState.Success(
                        profileId = profileId,
                        range = range,
                        selectedType = types.firstOrNull { it.type.name == selectedTypeName }?.type,
                        types = types,
                        library = library,
                        activity = activity,
                        incognito = incognito,
                    )
                }.distinctUntilChanged().flowOn(Dispatchers.IO)
            }.collect { event ->
                when (event) {
                    is ProfileScopedStateEvent.Reset -> mutableState.update { StatsScreenState.Loading }
                    is ProfileScopedStateEvent.Value -> mutableState.update { event.value }
                }
            }
        }
        screenModelScope.launchIO {
            while (true) {
                val now = ZonedDateTime.now()
                val nextMidnight = now.toLocalDate().plusDays(1L).atStartOfDay(now.zone)
                delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1_000L) + 250L)
                refreshToday()
            }
        }
    }

    fun setRange(range: StatsRange) {
        statisticsPreferences.selectedRange.set(range.name)
    }

    fun setType(type: eu.kanade.tachiyomi.source.entry.EntryType?) {
        statisticsPreferences.selectedType.set(type?.name.orEmpty())
    }

    fun retryActivity() {
        val failedTarget = when (val activity = (state.value as? StatsScreenState.Success)?.activity) {
            is ActivityState.Available -> activity.failedTarget
            is ActivityState.Failed -> activity.target
            else -> null
        }
        if (failedTarget != null) {
            finiteWindowEndDate.value = failedTarget.endDate.takeUnless { failedTarget.isLatest }
            statisticsPreferences.selectedRange.set(failedTarget.range.name)
        }
        activityReload.update { it + 1L }
    }

    fun navigateActivityByBuckets(bucketCount: Int) {
        val activity = (state.value as? StatsScreenState.Success)
            ?.activity
            ?.let { it as? ActivityState.Available }
            ?: return
        if (activity.loadingTarget != null || activity.data.window.range == StatsRange.ALL) return

        val latestEndDate = today.value
        val target = activity.data.window
            .shiftedByBuckets(bucketCount)
            .clampedTo(activity.data.trackingStartDate, latestEndDate)
        if (target.endDate == activity.data.window.endDate) return
        finiteWindowEndDate.value = target.endDate.takeUnless { it == latestEndDate }
    }

    fun showToday() {
        refreshToday()
        finiteWindowEndDate.value = null
    }

    fun refreshToday() {
        today.value = LocalDate.now()
    }

    private fun restoreDisplayedActivitySelectionAfterFailure(activity: ActivityState) {
        val available = activity as? ActivityState.Available ?: return
        if (available.failedTarget == null) return
        val displayedWindow = available.data.window
        finiteWindowEndDate.value = displayedWindow.endDate.takeUnless { displayedWindow.isLatest }
        statisticsPreferences.selectedRange.set(displayedWindow.range.name)
    }
}
