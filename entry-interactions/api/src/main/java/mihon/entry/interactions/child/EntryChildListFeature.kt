package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** Feature-owned boundary for child ordering, display construction, and optional progress labels. */
interface EntryChildListFeature {
    fun isApplicable(type: EntryType): Boolean

    fun readingOrder(
        entry: Entry,
        chapters: List<EntryChapter>,
        memberIds: List<Long>,
    ): EntryChildOrderResult

    fun firstReadingChild(
        entry: Entry,
        chapters: List<EntryChapter>,
        memberIds: List<Long>,
    ): EntryFirstChildResult

    fun displayOrder(
        entry: Entry,
        chapters: List<EntryChapter>,
        memberIds: List<Long>,
    ): EntryChildOrderResult

    fun displayList(request: EntryChildListRequest): EntryChildListResult

    fun progressLabels(request: EntryChildProgressRequest): EntryChildProgressResult
}

sealed interface EntryChildOrderResult {
    data class Available(val chapters: List<EntryChapter>) : EntryChildOrderResult
    data class Inapplicable(val type: EntryType) : EntryChildOrderResult
}

sealed interface EntryFirstChildResult {
    data class Available(val chapter: EntryChapter?) : EntryFirstChildResult
    data class Inapplicable(val type: EntryType) : EntryFirstChildResult
}

sealed interface EntryChildListResult {
    data class Available(val display: EntryChildListDisplay) : EntryChildListResult
    data class Inapplicable(val type: EntryType) : EntryChildListResult
}

sealed interface EntryChildProgressResult {
    data class Available(
        val labels: Flow<Map<Long, EntryChildProgressLabel>>,
    ) : EntryChildProgressResult

    data class Inapplicable(val type: EntryType) : EntryChildProgressResult
}
