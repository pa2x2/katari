package mihon.entry.interactions.host

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import tachiyomi.data.query.chunkedForSqlQuery
import tachiyomi.domain.entry.model.Entry

internal fun observeDuplicateCandidateEntries(
    currentEntry: Flow<Entry>,
    libraryEntries: Flow<List<Entry>>,
    observeCounts: (List<Long>) -> Flow<Map<Long, Long>>,
): Flow<DuplicateCandidateEntries> {
    return combine(currentEntry, libraryEntries) { current, library ->
        require(library.all { it.profileId == current.profileId }) {
            "Merge candidate data cannot cross profiles"
        }
        DuplicateCandidateEntrySet(
            current = current,
            library = library.filter { it.type == current.type },
        )
    }.distinctUntilChanged().flatMapLatest { entries ->
        val entryIds = (entries.library.map(Entry::id) + entries.current.id).distinct()
        observeCounts(entryIds).map { counts ->
            DuplicateCandidateEntries(
                current = entries.current,
                library = entries.library,
                counts = counts,
            )
        }
    }
}

internal fun observeDuplicateCandidateCounts(
    invalidations: Flow<Unit>,
    loadCounts: suspend () -> Map<Long, Long>,
): Flow<Map<Long, Long>> {
    return invalidations.mapLatest { loadCounts() }.distinctUntilChanged()
}

internal suspend fun loadDuplicateCandidateCounts(
    entryIds: List<Long>,
    loadChunk: suspend (List<Long>) -> Map<Long, Long>,
): Map<Long, Long> {
    return buildMap {
        entryIds.chunkedForSqlQuery().forEach { entryIdChunk ->
            putAll(loadChunk(entryIdChunk))
        }
    }
}

private data class DuplicateCandidateEntrySet(
    val current: Entry,
    val library: List<Entry>,
)

internal data class DuplicateCandidateEntries(
    val current: Entry,
    val library: List<Entry>,
    val counts: Map<Long, Long>,
)
