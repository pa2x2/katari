package mihon.feature.migration.list.models

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import mihon.entry.interactions.EntryMigrationSubject
import tachiyomi.domain.entry.model.Entry
import kotlin.coroutines.CoroutineContext

class MigratingEntry(
    val subject: EntryMigrationSubject,
    val entry: Entry,
    val chapterCount: Int,
    val latestChapter: Double?,
    val source: String,
    parentContext: CoroutineContext,
) {
    val migrationScope = CoroutineScope(parentContext + SupervisorJob() + Dispatchers.Default)

    val searchResult = MutableStateFlow<SearchResult>(SearchResult.Searching)

    sealed interface SearchResult {
        data object Searching : SearchResult
        data object NotFound : SearchResult
        data class Success(
            val entry: Entry,
            val chapterCount: Int,
            val latestChapter: Double?,
            val source: String,
        ) : SearchResult
    }
}
