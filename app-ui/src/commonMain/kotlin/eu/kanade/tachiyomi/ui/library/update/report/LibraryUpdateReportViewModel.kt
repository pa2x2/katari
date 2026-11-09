package eu.kanade.tachiyomi.ui.library.update.report

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.entry.interactions.migration.EntryMigrationFeature
import app.katari.entry.interactions.migration.EntryMigrationSelectionResult
import app.katari.entry.interactions.migration.EntryMigrationSubject
import app.katari.entry.interactions.source.EntryWebViewFeature
import app.katari.entry.interactions.source.EntryWebViewResolution
import app.katari.library.update.pause.LibrarySourcePauses
import app.katari.library.update.report.LibraryUpdateReport
import app.katari.sync.client.libraryupdate.OtherDeviceLibraryUpdate
import app.katari.ui.di.AppUiScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.tachiyomi.source.visualName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryUpdateQueue
import tachiyomi.domain.library.update.model.EntryUpdateMode
import tachiyomi.domain.library.update.model.LibraryUpdateRunSummary
import tachiyomi.domain.library.update.model.LibraryUpdateStart
import tachiyomi.domain.library.update.model.LibraryUpdateState
import tachiyomi.domain.library.update.model.SourceUpdatePause
import tachiyomi.domain.library.update.model.subscribeLatestSummary
import tachiyomi.domain.library.update.repository.LibraryUpdateReportRepository
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository
import tachiyomi.domain.source.service.SourceManager

@OptIn(ExperimentalCoroutinesApi::class)
@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class LibraryUpdateReportViewModel(
    private val libraryUpdateQueue: LibraryUpdateQueue,
    private val reportRepository: LibraryUpdateReportRepository,
    private val rulesRepository: LibraryUpdateRulesRepository,
    private val entryRepository: EntryRepository,
    private val libraryPreferences: LibraryPreferences,
    private val sourceManager: SourceManager,
    private val migrationFeature: EntryMigrationFeature,
    private val webViewFeature: EntryWebViewFeature,
    private val activeProfileProvider: ActiveProfileProvider,
    newerOtherDeviceUpdate: NewerOtherDeviceUpdate,
) : ViewModel() {

    val state: StateFlow<LibraryUpdateReportViewModel.State>
        field = MutableStateFlow<LibraryUpdateReportViewModel.State>(State.Loading)

    private val sourcePauses = LibrarySourcePauses(libraryPreferences)

    init {
        // The report describes a finished run, so pausing from it only shows up through the current rules.
        val pauses = sourcePauses.changes()
        val reports = combine(
            reportRepository.subscribeLatestRun(),
            reportRepository.subscribeLatestRunStatuses(),
            pauses.map { it.keys }.distinctUntilChanged(),
            ::Triple,
        )
            .mapLatest { (run, statuses, pausedSourceIds) ->
                if (run == null) return@mapLatest null
                val entries = entryRepository.getEntriesByIds(statuses.map { it.entryId }).associateBy(Entry::id)
                resolve(LibraryUpdateReport.build(run, statuses, entries, pausedSourceIds))
            }
        val pausedEntryIds = rulesRepository.subscribeEntryModes()
            .map { modes -> modes.filterValues { it == EntryUpdateMode.NEVER }.keys }
        // Rechecks started from the report fold into it without starting a run of their own.
        val isUpdating = activeProfileProvider.activeProfileIdFlow
            .flatMapLatest { libraryUpdateQueue.stateFlow(it) }
            .map { it is LibraryUpdateState.Running }
        val otherDevice = combine(newerOtherDeviceUpdate.subscribe(), reportRepository.subscribeLatestSummary()) {
                update,
                here,
            ->
            update?.let { OtherDeviceReport(it, here) }
        }
        viewModelScope.launch {
            combine(reports, pauses, pausedEntryIds, isUpdating, otherDevice) {
                    latest,
                    sourcePausesNow,
                    entryIds,
                    updating,
                    elsewhere,
                ->
                latest?.copy(
                    sourcePauses = sourcePausesNow,
                    pausedEntryIds = entryIds,
                    isUpdating = updating,
                    otherDevice = elsewhere,
                )
                    ?: State.Empty(elsewhere)
            }
                .collectLatest { newState -> state.value = newState }
        }
    }

    // Actions that can't work for an entry are left out of the report instead of doing nothing when tapped.
    private fun resolve(report: LibraryUpdateReport): State.Ready {
        val failures = report.failingRepeatedly + report.failingSources.flatMap { it.items }
        val sourceNames = (report.failingSources.map { it.sourceId } + report.pausedSources.map { it.sourceId })
            .map(sourceManager::getDisplayInfo)
            .filter { it.hasKnownName }
            .associate { it.id to it.visualName() }
        return State.Ready(
            report = report.copy(
                pausedSources = report.pausedSources.sortedBy {
                    sourceNames[it.sourceId]?.lowercase()
                },
            ),
            sourceNames = sourceNames,
            webViews = report.failingRepeatedly.mapNotNull { item ->
                (webViewFeature.resolveEntry(item.entry) as? EntryWebViewResolution.Available)
                    ?.let { item.entry.id to it }
            }
                .toMap(),
            migrations = failures.mapNotNull { item ->
                (migrationFeature.prepareSelection(listOf(item.entry)) as? EntryMigrationSelectionResult.Ready)
                    ?.subjects
                    ?.singleOrNull()
                    ?.let { item.entry.id to it }
            }
                .toMap(),
            sourcePauses = emptyMap(),
            pausedEntryIds = emptySet(),
            isUpdating = false,
            otherDevice = null,
        )
    }

    /** Pausing leaves the entry out of every library update until its mode is changed back. */
    fun setEntryPaused(entry: Entry, paused: Boolean) {
        val mode = if (paused) EntryUpdateMode.NEVER else EntryUpdateMode.FOLLOW_RULES
        viewModelScope.launch { rulesRepository.setEntryMode(listOf(entry.id), mode) }
    }

    /** @param until when the pause ends, in epoch milliseconds, or null to pause until the source is resumed. */
    fun pauseSource(sourceId: Long, until: Long?) {
        sourcePauses.pause(sourceId, until)
    }

    fun resumeSource(sourceId: Long) {
        sourcePauses.resume(listOf(sourceId))
    }

    suspend fun retry(entries: List<Entry>): LibraryUpdateStart {
        return libraryUpdateQueue.startSelection(entries.map { it.id })
    }

    suspend fun checkSkipped(): LibraryUpdateStart = libraryUpdateQueue.startSkippedOfLatest()

    /** Another device's update of the whole library, newer than any here, with this device's own latest one. */
    @Immutable
    data class OtherDeviceReport(
        val update: OtherDeviceLibraryUpdate,
        val here: LibraryUpdateRunSummary?,
    )

    sealed interface State {
        /** Shown in place of this device's report unless that is asked for. */
        val otherDevice: OtherDeviceReport?

        data object Loading : State {
            override val otherDevice: OtherDeviceReport? = null
        }

        data class Empty(override val otherDevice: OtherDeviceReport?) : State

        @Immutable
        data class Ready(
            val report: LibraryUpdateReport,
            /** By source id; a source whose name was never recorded has none. */
            val sourceNames: Map<Long, String>,
            /** By entry id, for the failing entries whose page can be opened. */
            val webViews: Map<Long, EntryWebViewResolution.Available>,
            /** By entry id, for the failing entries that can be migrated. */
            val migrations: Map<Long, EntryMigrationSubject>,
            /** By source id, the pauses in effect now. */
            val sourcePauses: Map<Long, SourceUpdatePause>,
            val pausedEntryIds: Set<Long>,
            val isUpdating: Boolean,
            override val otherDevice: OtherDeviceReport?,
        ) : State
    }
}
