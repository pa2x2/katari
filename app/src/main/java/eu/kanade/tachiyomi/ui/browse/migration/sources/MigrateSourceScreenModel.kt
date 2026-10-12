package eu.kanade.tachiyomi.ui.browse.migration.sources

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.source.interactor.GetSourcesWithFavoriteCount
import eu.kanade.domain.source.interactor.SetMigrateSorting
import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import logcat.LogPriority
import mihon.entry.interactions.migration.EntryMigrationAvailability
import mihon.entry.interactions.migration.EntryMigrationFeature
import mihon.feature.migration.session.SourceMigrationSessionStore
import mihon.feature.migration.session.model.SourceMigrationSessionId
import mihon.feature.migration.session.model.SourceMigrationSessionStage
import mihon.feature.profiles.core.ProfileManager
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Duration.Companion.seconds

class MigrateSourceScreenModel(
    private val preferences: SourcePreferences = Injekt.get(),
    private val getSourcesWithFavoriteCount: GetSourcesWithFavoriteCount = Injekt.get(),
    private val entryRepository: EntryRepository = Injekt.get(),
    private val migration: EntryMigrationFeature = Injekt.get(),
    private val sessionStore: SourceMigrationSessionStore = Injekt.get(),
    private val profileManager: ProfileManager = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
) : ScreenModel {

    private val _channel = Channel<Event>(Int.MAX_VALUE)
    val channel = _channel.receiveAsFlow()

    private val sourcesAndSessions = combine(
        getSourcesWithFavoriteCount.subscribe(),
        entryRepository.getLibraryEntriesAsFlow(),
        profileManager.activeProfile
            .map { profile -> profile?.id ?: profileManager.activeProfileId }
            .distinctUntilChanged()
            .flatMapLatest(sessionStore::observeActive),
    ) { sources, entries, sessions ->
        val migratableCounts = entries
            .asSequence()
            .filter { migration.availability(it) is EntryMigrationAvailability.Available }
            .groupingBy { it.source }
            .eachCount()
        val migrationSources = sources.mapNotNull { (source, _) ->
            migratableCounts[source.id]?.let { source to it.toLong() }
        }.let(getSourcesWithFavoriteCount::sort)
        val activeSessions = sessions.map { session ->
            ActiveSession(
                id = session.id,
                sourceName = sourceManager.getDisplayInfo(session.originSourceId).name,
                stage = session.stage,
            )
        }
        migrationSources to activeSessions
    }
        .catch {
            logcat(LogPriority.ERROR, it)
            _channel.send(Event.FailedFetchingSourcesWithCount)
        }

    val state: StateFlow<State> = combine(
        sourcesAndSessions,
        preferences.migrationSortingMode.changes(),
        preferences.migrationSortingDirection.changes(),
    ) { (sources, activeSessions), sortingMode, sortingDirection ->
        State(
            isLoading = false,
            items = sources,
            activeSessions = activeSessions,
            sortingMode = sortingMode,
            sortingDirection = sortingDirection,
        )
    }
        .flowOn(Dispatchers.IO)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5.seconds), State())

    fun toggleSortingMode() {
        preferences.migrationSortingMode.getAndSet { mode ->
            when (mode) {
                SetMigrateSorting.Mode.ALPHABETICAL -> SetMigrateSorting.Mode.TOTAL
                SetMigrateSorting.Mode.TOTAL -> SetMigrateSorting.Mode.ALPHABETICAL
            }
        }
    }

    fun toggleSortingDirection() {
        preferences.migrationSortingDirection.getAndSet { direction ->
            when (direction) {
                SetMigrateSorting.Direction.ASCENDING -> SetMigrateSorting.Direction.DESCENDING
                SetMigrateSorting.Direction.DESCENDING -> SetMigrateSorting.Direction.ASCENDING
            }
        }
    }

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val items: List<Pair<Source, Long>> = listOf(),
        val activeSessions: List<ActiveSession> = emptyList(),
        val sortingMode: SetMigrateSorting.Mode = SetMigrateSorting.Mode.ALPHABETICAL,
        val sortingDirection: SetMigrateSorting.Direction = SetMigrateSorting.Direction.ASCENDING,
    ) {
        val isEmpty = items.isEmpty() && activeSessions.isEmpty()
    }

    @Immutable
    data class ActiveSession(
        val id: SourceMigrationSessionId,
        val sourceName: String,
        val stage: SourceMigrationSessionStage,
    )

    sealed interface Event {
        data object FailedFetchingSourcesWithCount : Event
    }
}
