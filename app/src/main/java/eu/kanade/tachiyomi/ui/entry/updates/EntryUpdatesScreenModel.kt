package eu.kanade.tachiyomi.ui.entry.updates

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.library.update.model.EntryUpdateMode
import tachiyomi.domain.library.update.model.EntryUpdateStatus
import tachiyomi.domain.library.update.repository.LibraryUpdateReportRepository
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** How library updates treat one entry, and what the latest update that considered it decided. */
class EntryUpdatesScreenModel(
    private val entryId: Long,
    private val rulesRepository: LibraryUpdateRulesRepository = Injekt.get(),
    reportRepository: LibraryUpdateReportRepository = Injekt.get(),
    categoryRepository: CategoryRepository = Injekt.get(),
) : StateScreenModel<EntryUpdatesScreenModel.State>(State()) {

    init {
        screenModelScope.launch {
            combine(
                rulesRepository.subscribeEntryMode(entryId),
                reportRepository.subscribeStatus(entryId),
                categoryRepository.getAllAsFlow(),
            ) { mode, status, categories ->
                State(mode = mode, status = status, categories = categories.associateBy(Category::id))
            }
                .collectLatest { state -> mutableState.update { state } }
        }
    }

    fun setMode(mode: EntryUpdateMode) {
        screenModelScope.launch { rulesRepository.setEntryMode(listOf(entryId), mode) }
    }

    @Immutable
    data class State(
        val mode: EntryUpdateMode = EntryUpdateMode.FOLLOW_RULES,
        /** Null until a library update has considered the entry. */
        val status: EntryUpdateStatus? = null,
        /** For naming the category a decision came from. */
        val categories: Map<Long, Category> = emptyMap(),
    )
}
