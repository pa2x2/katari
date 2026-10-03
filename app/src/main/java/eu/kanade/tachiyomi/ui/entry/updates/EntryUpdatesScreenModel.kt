package eu.kanade.tachiyomi.ui.entry.updates

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.source.visualName
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mihon.feature.library.update.pause.LibrarySourcePauses
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.update.model.EntryUpdateMode
import tachiyomi.domain.library.update.model.EntryUpdateStatus
import tachiyomi.domain.library.update.repository.LibraryUpdateReportRepository
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * How library updates treat one entry, and what the latest update that considered it decided.
 *
 * @param memberIds the entry's merged members, or the entry alone, whose sources may be paused.
 */
class EntryUpdatesScreenModel(
    private val entryId: Long,
    memberIds: List<Long>,
    private val rulesRepository: LibraryUpdateRulesRepository = Injekt.get(),
    reportRepository: LibraryUpdateReportRepository = Injekt.get(),
    categoryRepository: CategoryRepository = Injekt.get(),
    entryRepository: EntryRepository = Injekt.get(),
    libraryPreferences: LibraryPreferences = Injekt.get(),
    sourceManager: SourceManager = Injekt.get(),
) : StateScreenModel<EntryUpdatesScreenModel.State>(State()) {

    private val sourcePauses = LibrarySourcePauses(libraryPreferences)

    init {
        val sourceIds = flow { emit(entryRepository.getEntriesByIds(memberIds).map { it.source }.distinct()) }
        screenModelScope.launch {
            combine(
                rulesRepository.subscribeEntryMode(entryId),
                reportRepository.subscribeStatus(entryId),
                categoryRepository.getAllAsFlow(),
                sourceIds,
                sourcePauses.changes(),
            ) { mode, status, categories, entrySourceIds, pauses ->
                State(
                    mode = mode,
                    status = status,
                    categories = categories.associateBy(Category::id),
                    pausedSources = entrySourceIds.mapNotNull { sourceId ->
                        pauses[sourceId]?.let {
                            PausedSource(sourceId, sourceManager.getDisplayInfo(sourceId).visualName(), it.until)
                        }
                    },
                )
            }
                .collectLatest { state -> mutableState.update { state } }
        }
    }

    fun setMode(mode: EntryUpdateMode) {
        screenModelScope.launch { rulesRepository.setEntryMode(listOf(entryId), mode) }
    }

    fun resumeSource(sourceId: Long) {
        sourcePauses.resume(listOf(sourceId))
    }

    /** @param until when the pause ends, or null when it lasts until the source is resumed. */
    data class PausedSource(val id: Long, val name: String, val until: Long?)

    @Immutable
    data class State(
        val mode: EntryUpdateMode = EntryUpdateMode.FOLLOW_RULES,
        /** Null until a library update has considered the entry. */
        val status: EntryUpdateStatus? = null,
        /** For naming the category a decision came from. */
        val categories: Map<Long, Category> = emptyMap(),
        val pausedSources: List<PausedSource> = emptyList(),
    )
}
