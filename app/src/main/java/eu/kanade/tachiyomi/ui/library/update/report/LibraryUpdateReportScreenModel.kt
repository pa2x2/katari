package eu.kanade.tachiyomi.ui.library.update.report

import android.app.Application
import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.source.visualName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mihon.entry.interactions.migration.EntryMigrationFeature
import mihon.entry.interactions.migration.EntryMigrationSelectionResult
import mihon.entry.interactions.migration.EntryMigrationSubject
import mihon.entry.interactions.source.EntryWebViewFeature
import mihon.entry.interactions.source.EntryWebViewResolution
import mihon.feature.library.update.pause.LibrarySourcePauses
import mihon.feature.library.update.report.LibraryUpdateReport
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.update.model.EntryUpdateMode
import tachiyomi.domain.library.update.model.SourceUpdatePause
import tachiyomi.domain.library.update.repository.LibraryUpdateReportRepository
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryUpdateReportScreenModel(
    private val application: Application = Injekt.get(),
    private val reportRepository: LibraryUpdateReportRepository = Injekt.get(),
    private val rulesRepository: LibraryUpdateRulesRepository = Injekt.get(),
    private val entryRepository: EntryRepository = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val migrationFeature: EntryMigrationFeature = Injekt.get(),
    private val webViewFeature: EntryWebViewFeature = Injekt.get(),
) : StateScreenModel<LibraryUpdateReportScreenModel.State>(State.Loading) {

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
        val isUpdating = LibraryUpdateJob.progressFlow(application).map { it != null }
        screenModelScope.launch {
            combine(reports, pauses, pausedEntryIds, isUpdating) { latest, sourcePausesNow, entryIds, updating ->
                latest?.copy(sourcePauses = sourcePausesNow, pausedEntryIds = entryIds, isUpdating = updating)
                    ?: State.Empty
            }
                .collectLatest { state -> mutableState.update { state } }
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
        )
    }

    /** Pausing leaves the entry out of every library update until its mode is changed back. */
    fun setEntryPaused(entry: Entry, paused: Boolean) {
        val mode = if (paused) EntryUpdateMode.NEVER else EntryUpdateMode.FOLLOW_RULES
        screenModelScope.launch { rulesRepository.setEntryMode(listOf(entry.id), mode) }
    }

    /** @param until when the pause ends, in epoch milliseconds, or null to pause until the source is resumed. */
    fun pauseSource(sourceId: Long, until: Long?) {
        sourcePauses.pause(sourceId, until)
    }

    fun resumeSource(sourceId: Long) {
        sourcePauses.resume(listOf(sourceId))
    }

    /** @return false when another update is already running. */
    suspend fun retry(entries: List<Entry>): Boolean {
        return LibraryUpdateJob.startSelection(application, entries.map { it.id })
    }

    suspend fun checkSkipped(): Boolean = LibraryUpdateJob.startSkippedOfLatest(application)

    sealed interface State {
        data object Loading : State

        data object Empty : State

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
        ) : State
    }
}
