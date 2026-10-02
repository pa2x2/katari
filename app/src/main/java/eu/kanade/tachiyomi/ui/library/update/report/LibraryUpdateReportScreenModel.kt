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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mihon.entry.interactions.migration.EntryMigrationFeature
import mihon.entry.interactions.migration.EntryMigrationSelectionResult
import mihon.entry.interactions.migration.EntryMigrationSubject
import mihon.entry.interactions.source.EntryWebViewFeature
import mihon.entry.interactions.source.EntryWebViewResolution
import mihon.feature.library.update.report.LibraryUpdateReport
import tachiyomi.core.common.preference.minusAssign
import tachiyomi.core.common.preference.plusAssign
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.update.model.EntryUpdateMode
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

    init {
        val reports = combine(
            reportRepository.subscribeLatestRun(),
            reportRepository.subscribeLatestRunStatuses(),
            ::Pair,
        )
            .mapLatest { (run, statuses) ->
                if (run == null) return@mapLatest null
                val entries = entryRepository.getEntriesByIds(statuses.map { it.entryId }).associateBy(Entry::id)
                resolve(LibraryUpdateReport.build(run, statuses, entries))
            }
        // The report describes a finished run, so pausing from it only shows up through the current rules.
        val pausedSourceIds = libraryPreferences.updateExcludedSources.changes()
            .map { ids -> ids.mapNotNull(String::toLongOrNull).toSet() }
        val pausedEntryIds = rulesRepository.subscribeEntryModes()
            .map { modes -> modes.filterValues { it == EntryUpdateMode.NEVER }.keys }
        // Rechecks started from the report fold into it without starting a run of their own.
        val isUpdating = LibraryUpdateJob.progressFlow(application).map { it != null }
        screenModelScope.launch {
            combine(reports, pausedSourceIds, pausedEntryIds, isUpdating) { latest, sourceIds, entryIds, updating ->
                latest?.copy(pausedSourceIds = sourceIds, pausedEntryIds = entryIds, isUpdating = updating)
                    ?: State.Empty
            }
                .collectLatest { state -> mutableState.update { state } }
        }
    }

    // Actions that can't work for an entry are left out of the report instead of doing nothing when tapped.
    private fun resolve(report: LibraryUpdateReport): State.Ready {
        val failures = report.failingRepeatedly + report.failingSources.flatMap { it.items }
        return State.Ready(
            report = report,
            sourceNames = report.failingSources
                .map { sourceManager.getDisplayInfo(it.sourceId) }
                .filter { it.hasKnownName }
                .associate { it.id to it.visualName() },
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
            pausedSourceIds = emptySet(),
            pausedEntryIds = emptySet(),
            isUpdating = false,
        )
    }

    /** Pausing leaves the entry out of every library update until its mode is changed back. */
    fun setEntryPaused(entry: Entry, paused: Boolean) {
        val mode = if (paused) EntryUpdateMode.NEVER else EntryUpdateMode.FOLLOW_RULES
        screenModelScope.launch { rulesRepository.setEntryMode(listOf(entry.id), mode) }
    }

    fun setSourcePaused(sourceId: Long, paused: Boolean) {
        if (paused) {
            libraryPreferences.updateExcludedSources += sourceId.toString()
        } else {
            libraryPreferences.updateExcludedSources -= sourceId.toString()
        }
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
            val pausedSourceIds: Set<Long>,
            val pausedEntryIds: Set<Long>,
            val isUpdating: Boolean,
        ) : State
    }
}
