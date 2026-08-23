package eu.kanade.tachiyomi.ui.stats

import androidx.compose.ui.util.fastDistinctBy
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.more.stats.ActivityState
import eu.kanade.presentation.more.stats.StatsScreenState
import eu.kanade.presentation.more.stats.data.StatsLibrary
import eu.kanade.presentation.more.stats.data.StatsRange
import eu.kanade.presentation.more.stats.data.StatsType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature
import mihon.entry.interactions.presentation.EntryTypePresentationFeature
import mihon.entry.interactions.statistics.EntryStatisticsFeature
import mihon.entry.interactions.tracking.EntryTrackingAvailability
import mihon.entry.interactions.tracking.EntryTrackingFeature
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
import java.time.LocalDate
import java.util.Locale

class StatsScreenModel(
    private val activeProfileProvider: ActiveProfileProvider = Injekt.get(),
    private val getLibraryEntries: GetLibraryEntries = Injekt.get(),
    private val presentationFeature: EntryTypePresentationFeature = Injekt.get(),
    private val statisticsFeature: EntryStatisticsFeature = Injekt.get(),
    private val statisticsRepository: StatisticsRepository = Injekt.get(),
    private val statisticsPreferences: StatisticsPreferences = Injekt.get(),
    private val basePreferences: BasePreferences = Injekt.get(),
    private val downloadRuntime: EntryDownloadRuntimeFeature = Injekt.get(),
    private val trackingFeature: EntryTrackingFeature = Injekt.get(),
) : StateScreenModel<StatsScreenState>(StatsScreenState.Loading) {

    private val activityReload = MutableStateFlow(0L)
    private val types = buildStatisticsTypes(statisticsFeature, presentationFeature)

    init {
        screenModelScope.launchIO {
            observeProfileScopedState(activeProfileProvider.activeProfileIdFlow) { profileId ->
                combine(
                    combine(
                        getLibraryEntries.subscribe(profileId),
                        downloadRuntime.changes.onStart { emit(Unit) },
                        trackingFeature.observeCollection(),
                    ) { libraryEntries, _, tracking ->
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
                            coverageByType = buildLibraryCoverage(
                                items = distinctEntries,
                                types = types.map(StatsType::type),
                                downloadApplicable = downloadRuntime::isApplicable,
                                downloadCount = { item -> item.memberEntries.sumOf(downloadRuntime::downloadCount) },
                                trackingApplicable = { type ->
                                    trackingFeature.availability(type) is EntryTrackingAvailability.Available
                                },
                                connectedTrackingTypes = tracking.scoreSupportedEntryTypes,
                                trackedEntryIds = tracking.entries.filterValues { it.isNotEmpty() }.keys,
                            ),
                            insightsByType = distinctEntries
                                .groupBy { it.entry.type }
                                .mapValues { (_, items) -> buildLibraryInsights(items) },
                        )
                    },
                    combine(
                        statisticsPreferences.selectedRange.changes(),
                        activityReload,
                    ) { rangeName, _ ->
                        StatsRange.entries.find { it.name == rangeName } ?: StatsRange.THIRTY_DAYS
                    }.distinctUntilChanged().flatMapLatest { range ->
                        statisticsRepository.subscribeActivity(
                            profileId = profileId,
                            startLocalDate = range.startLocalDate(LocalDate.now())?.toString(),
                        ).map { snapshot ->
                            Pair<StatsRange, ActivityState>(
                                range,
                                ActivityState.Available(
                                    buildActivity(
                                        snapshot = snapshot,
                                        range = range,
                                        types = types.map(StatsType::type),
                                        today = LocalDate.now(),
                                        locale = Locale.getDefault(),
                                    ),
                                ),
                            )
                        }.onStart {
                            emit(range to ActivityState.Loading)
                        }.catch { error ->
                            logcat(LogPriority.ERROR, error)
                            emit(range to ActivityState.Failed)
                        }
                    },
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
    }

    fun setRange(range: StatsRange) {
        statisticsPreferences.selectedRange.set(range.name)
    }

    fun setType(type: eu.kanade.tachiyomi.source.entry.EntryType?) {
        statisticsPreferences.selectedType.set(type?.name.orEmpty())
    }

    fun retryActivity() {
        activityReload.update { it + 1L }
    }
}
