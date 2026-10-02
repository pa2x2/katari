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
                val report = LibraryUpdateReport.build(run, statuses, entries)
                val sourceNames = report.failingSources.associate { source ->
                    source.sourceId to sourceManager.getDisplayInfo(source.sourceId).visualName()
                }
                report to sourceNames
            }
        // The report describes a finished run, so pausing from it only shows up through the current rules.
        val pausedSourceIds = libraryPreferences.updateExcludedSources.changes()
            .map { ids -> ids.mapNotNull(String::toLongOrNull).toSet() }
        val pausedEntryIds = rulesRepository.subscribeEntryModes()
            .map { modes -> modes.filterValues { it == EntryUpdateMode.NEVER }.keys }
        screenModelScope.launch {
            combine(reports, pausedSourceIds, pausedEntryIds) { latest, sourceIds, entryIds ->
                val (report, sourceNames) = latest ?: return@combine State.Empty
                State.Ready(
                    report = report,
                    sourceNames = sourceNames,
                    pausedSourceIds = sourceIds,
                    pausedEntryIds = entryIds,
                )
            }
                .collectLatest { state -> mutableState.update { state } }
        }
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
    suspend fun retry(entry: Entry): Boolean = LibraryUpdateJob.startSelection(application, listOf(entry.id))

    suspend fun checkSkipped(): Boolean = LibraryUpdateJob.startSkippedOfLatest(application)

    fun migrationSubject(entry: Entry): EntryMigrationSubject? {
        return (migrationFeature.prepareSelection(listOf(entry)) as? EntryMigrationSelectionResult.Ready)
            ?.subjects
            ?.singleOrNull()
    }

    fun webView(entry: Entry): EntryWebViewResolution.Available? {
        return webViewFeature.resolveEntry(entry) as? EntryWebViewResolution.Available
    }

    sealed interface State {
        data object Loading : State

        data object Empty : State

        @Immutable
        data class Ready(
            val report: LibraryUpdateReport,
            val sourceNames: Map<Long, String>,
            val pausedSourceIds: Set<Long>,
            val pausedEntryIds: Set<Long>,
        ) : State
    }
}
