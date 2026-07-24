package mihon.entry.interactions

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

interface EntryChildListInteraction {
    fun sortedForReading(entry: Entry, chapters: List<EntryChapter>, memberIds: List<Long>): List<EntryChapter>
    fun sortedForDisplay(entry: Entry, chapters: List<EntryChapter>, memberIds: List<Long>): List<EntryChapter>
}

interface EntryChildProgressInteraction {
    fun progressLabels(request: EntryChildProgressRequest): Flow<Map<Long, EntryChildProgressLabel>>
}

fun interface EntryMissingChildGapInteraction {
    fun buildDisplayList(request: EntryChildListRequest): EntryChildListDisplay
}

interface EntryChildGroupFilterInteraction {
    fun groupFor(entry: Entry, chapter: EntryChapter): String?
    fun normalizeGroup(entry: Entry, group: String): String?
}
