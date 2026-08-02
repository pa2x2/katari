package eu.kanade.tachiyomi.ui.browse.migration.sources

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.source.interactor.GetSourcesWithFavoriteCount
import eu.kanade.domain.source.interactor.SetMigrateSorting
import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import mihon.entry.interactions.migration.EntryMigrationAvailability
import mihon.entry.interactions.migration.EntryMigrationFeature
import mihon.feature.migration.session.SourceMigrationSessionStore
import mihon.feature.migration.session.model.SourceMigrationSessionId
import mihon.feature.migration.session.model.SourceMigrationSessionStage
import mihon.feature.profiles.core.ProfileManager
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrateSourceScreenModel(
    preferences: SourcePreferences = Injekt.get(),
    private val getSourcesWithFavoriteCount: GetSourcesWithFavoriteCount = Injekt.get(),
    private val setMigrateSorting: SetMigrateSorting = Injekt.get(),
    private val entryRepository: EntryRepository = Injekt.get(),
    private val migration: EntryMigrationFeature = Injekt.get(),
    private val sessionStore: SourceMigrationSessionStore = Injekt.get(),
    private val profileManager: ProfileManager = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
) : StateScreenModel<MigrateSourceScreenModel.State>(State()) {

    private val _channel = Channel<Event>(Int.MAX_VALUE)
    val channel = _channel.receiveAsFlow()

    init {
        screenModelScope.launchIO {
            combine(
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
                .collectLatest { (sources, activeSessions) ->
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            items = sources,
                            activeSessions = activeSessions,
                        )
                    }
                }
        }

        preferences.migrationSortingDirection.changes()
            .onEach { mutableState.update { state -> state.copy(sortingDirection = it) } }
            .launchIn(screenModelScope)

        preferences.migrationSortingMode.changes()
            .onEach { mutableState.update { state -> state.copy(sortingMode = it) } }
            .launchIn(screenModelScope)
    }

    fun toggleSortingMode() {
        with(state.value) {
            val newMode = when (sortingMode) {
                SetMigrateSorting.Mode.ALPHABETICAL -> SetMigrateSorting.Mode.TOTAL
                SetMigrateSorting.Mode.TOTAL -> SetMigrateSorting.Mode.ALPHABETICAL
            }

            setMigrateSorting.await(newMode, sortingDirection)
        }
    }

    fun toggleSortingDirection() {
        with(state.value) {
            val newDirection = when (sortingDirection) {
                SetMigrateSorting.Direction.ASCENDING -> SetMigrateSorting.Direction.DESCENDING
                SetMigrateSorting.Direction.DESCENDING -> SetMigrateSorting.Direction.ASCENDING
            }

            setMigrateSorting.await(sortingMode, newDirection)
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
