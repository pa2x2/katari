package eu.kanade.tachiyomi.ui.stats

import android.app.Application
import androidx.compose.ui.util.fastDistinctBy
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.more.stats.ActivityState
import eu.kanade.presentation.more.stats.StatsScreenState
import eu.kanade.presentation.more.stats.data.StatsActivityWindow
import eu.kanade.presentation.more.stats.data.StatsDailyGoal
import eu.kanade.presentation.more.stats.data.StatsLibrary
import eu.kanade.presentation.more.stats.data.StatsRange
import eu.kanade.presentation.more.stats.data.StatsReadingCalendar
import eu.kanade.presentation.more.stats.data.StatsRecapNotifications
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.presentation.more.stats.layout.statisticsLayoutTab
import eu.kanade.tachiyomi.data.statistics.StatisticsRecapNotificationJob
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.stats.recap.delivery.subscribeUnopenedYearRecap
import eu.kanade.tachiyomi.ui.stats.recap.period.StatisticsRecapPeriod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import mihon.entry.interactions.presentation.EntryTypePresentationFeature
import mihon.entry.interactions.statistics.EntryStatisticsFeature
import mihon.feature.profiles.core.ProfileScopedStateEvent
import mihon.feature.profiles.core.ProfileStore
import mihon.feature.profiles.core.observeProfileScopedState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.entry.interactor.GetLibraryEntries
import tachiyomi.domain.statistics.model.StatisticsActivityTimeline
import tachiyomi.domain.statistics.model.StatisticsCardLayout
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
    private val profileStore: ProfileStore = Injekt.get(),
    private val basePreferences: BasePreferences = Injekt.get(),
    private val application: Application = Injekt.get(),
) : StateScreenModel<StatsScreenState>(StatsScreenState.Loading) {

    private val activityReload = MutableStateFlow(0L)
    private val today = MutableStateFlow(LocalDate.now())
    private val finiteWindowSelection = MutableStateFlow<StatsActivityWindow?>(null)
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
                                .mapValues { (_, items) -> buildLibraryInsights(items, today.value) },
                        )
                    },
                    combine(
                        statisticsPreferences.selectedRange.changes(),
                        finiteWindowSelection,
                        today,
                        activityReload,
                    ) { rangeName, historicalSelection, today, reloadToken ->
                        val range = StatsRange.entries.find { it.name == rangeName } ?: StatsRange.THIRTY_DAYS
                        StatisticsActivityLoadRequest(
                            window = range.windowForSelection(historicalSelection, today),
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
                    combine(
                        statisticsPreferences.selectedType.changes(),
                        basePreferences.incognitoMode.changes(),
                        subscribeReadingCalendar(profileId),
                        ::Triple,
                    ),
                    combine(
                        (listOf(null) + types.map { it.type }).map(::statisticsLayoutTab).map { tab ->
                            StatisticsPreferences(profileStore.profileStore(profileId)).cardLayout(tab).changes()
                                .map { tab to StatisticsCardLayout.decode(it) }
                        },
                    ) { it.toMap() },
                    combine(
                        subscribeDailyGoal(profileId),
                        StatisticsPreferences(profileStore.profileStore(profileId)).let { preferences ->
                            combine(
                                preferences.monthlyRecapNotification.changes(),
                                preferences.yearlyRecapNotification.changes(),
                                ::StatsRecapNotifications,
                            )
                        },
                        ::Pair,
                    ),
                ) { library, (range, activity), (selectedTypeName, incognito, calendar), cardLayouts, (goal, recap) ->
                    StatsScreenState.Success(
                        profileId = profileId,
                        range = range,
                        selectedType = types.firstOrNull { it.type.name == selectedTypeName }?.type,
                        types = types,
                        library = library,
                        activity = activity,
                        incognito = incognito,
                        cardLayouts = cardLayouts,
                        calendar = calendar,
                        goalMinutes = goal.first,
                        goal = goal.second,
                        recapNotifications = recap,
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

    /** The reading calendar always ends today, independent of the selected range or navigated window. */
    private fun subscribeReadingCalendar(profileId: Long): Flow<StatsReadingCalendar?> =
        today.flatMapLatest { day ->
            val start = readingCalendarStart(day, Locale.getDefault())
            statisticsRepository.subscribeActivityTimeline(
                profileId = profileId,
                startLocalDate = start.toString(),
                endLocalDate = day.toString(),
            ).map<StatisticsActivityTimeline, StatsReadingCalendar?> { timeline ->
                buildReadingCalendar(timeline, start, day)
            }.catch { error ->
                logcat(LogPriority.ERROR, error)
                emit(null)
            }
        }

    /** The goal setting with today's progress toward it; progress is null while no goal is set. */
    private fun subscribeDailyGoal(profileId: Long): Flow<Pair<Int, StatsDailyGoal?>> =
        StatisticsPreferences(profileStore.profileStore(profileId)).dailyGoalMinutes.changes()
            .combine(today, ::Pair)
            .flatMapLatest { (minutes, day) ->
                if (minutes <= 0) return@flatMapLatest flowOf(minutes to null)
                statisticsRepository.subscribeActivityTimeline(
                    profileId = profileId,
                    startLocalDate = null,
                    endLocalDate = day.toString(),
                ).map<StatisticsActivityTimeline, Pair<Int, StatsDailyGoal?>> { timeline ->
                    minutes to buildDailyGoal(timeline, minutes * 60_000L, day)
                }.catch { error ->
                    logcat(LogPriority.ERROR, error)
                    emit(minutes to null)
                }
            }

    /** The year recap that's new and unopened, shown as a banner until it's opened. */
    val newYearRecap: StateFlow<StatisticsRecapPeriod.Year?> = subscribeUnopenedYearRecap()
        .catch { error ->
            logcat(LogPriority.ERROR, error)
            emit(null)
        }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5_000L), null)

    fun setDailyGoal(profileId: Long, minutes: Int) {
        if ((state.value as? StatsScreenState.Success)?.profileId != profileId) return
        StatisticsPreferences(profileStore.profileStore(profileId)).dailyGoalMinutes.set(minutes)
    }

    fun setRecapNotifications(profileId: Long, notifications: StatsRecapNotifications) {
        if ((state.value as? StatsScreenState.Success)?.profileId != profileId) return
        StatisticsRecapNotificationJob.setEnabled(application, profileId, notifications)
    }

    fun setCardLayout(profileId: Long, tab: String, layout: StatisticsCardLayout) {
        if ((state.value as? StatsScreenState.Success)?.profileId != profileId) return
        StatisticsPreferences(profileStore.profileStore(profileId)).cardLayout(tab).set(layout.encode())
    }

    fun setRange(range: StatsRange) {
        statisticsPreferences.selectedRange.set(range.name)
    }

    fun setType(type: EntryType?) {
        statisticsPreferences.selectedType.set(type?.name.orEmpty())
    }

    fun retryActivity() {
        val failedTarget = when (val activity = (state.value as? StatsScreenState.Success)?.activity) {
            is ActivityState.Available -> activity.failedTarget
            is ActivityState.Failed -> activity.target
            else -> null
        }
        if (failedTarget != null) {
            finiteWindowSelection.value = failedTarget.takeUnless { it.isLatest }
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
        finiteWindowSelection.value = target.takeUnless { it.isLatest }
    }

    fun showToday() {
        refreshToday()
        finiteWindowSelection.value = null
    }

    fun refreshToday() {
        today.value = LocalDate.now()
    }

    private fun restoreDisplayedActivitySelectionAfterFailure(activity: ActivityState) {
        val available = activity as? ActivityState.Available ?: return
        if (available.failedTarget == null) return
        val displayedWindow = available.data.window
        finiteWindowSelection.value = displayedWindow.takeUnless { it.isLatest }
        statisticsPreferences.selectedRange.set(displayedWindow.range.name)
    }
}
