package tachiyomi.domain.entry.interactor

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import tachiyomi.domain.entry.interactor.GetDuplicateLibraryEntries
import tachiyomi.domain.entry.model.DuplicateEntryCandidate
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.library.service.DuplicatePreferences

class GetEnhancedDuplicateLibraryEntries(
    private val application: Application,
    private val getDuplicateLibraryEntries: GetDuplicateLibraryEntries,
    private val enhanceDuplicateLibraryEntries: EnhanceDuplicateLibraryEntries,
    private val duplicatePreferences: DuplicatePreferences,
) {

    suspend operator fun invoke(entry: Entry): List<DuplicateEntryCandidate> {
        if (!entry.initialized) return emptyList()

        val duplicates = getDuplicateLibraryEntries(entry)
        return enhanceDuplicateLibraryEntries(application, entry, duplicates)
    }

    fun subscribe(
        entry: Flow<Entry>,
        scope: CoroutineScope,
    ): StateFlow<List<DuplicateEntryCandidate>> {
        return entry
            .distinctUntilChanged()
            .flatMapLatest { currentEntry ->
                if (!currentEntry.initialized) {
                    flowOf(emptyList())
                } else {
                    combine(
                        getDuplicateLibraryEntries.subscribe(flowOf(currentEntry), scope),
                        enhancementConfigFlow(),
                    ) { candidates, _ -> candidates }
                        .mapLatest { candidates ->
                            enhanceDuplicateLibraryEntries(application, currentEntry, candidates)
                        }
                }
            }
            .distinctUntilChanged()
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
                initialValue = emptyList(),
            )
    }

    private fun enhancementConfigFlow(): Flow<Unit> {
        return combine(
            duplicatePreferences.extendedDuplicateDetectionEnabled.changes(),
            duplicatePreferences.coverWeight.changes(),
        ) { _, _ -> }
    }

    private companion object {
        private const val SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L
    }
}
