package mihon.entry.interactions.book

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import mihon.entry.interactions.EntryChildListProcessor
import mihon.entry.interactions.EntryChildProgressLabel
import mihon.entry.interactions.EntryChildProgressProcessor
import mihon.entry.interactions.EntryChildProgressRequest
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.service.sortedForMergedDisplay
import tachiyomi.domain.entry.service.sortedForReading
import tachiyomi.i18n.MR

internal class BookChildListProcessor(
    private val entryProgressRepository: EntryProgressRepository,
) : EntryChildListProcessor, EntryChildProgressProcessor {
    override val type = EntryType.BOOK

    override fun sortedForReading(
        entry: Entry,
        chapters: List<EntryChapter>,
        memberIds: List<Long>,
    ): List<EntryChapter> = chapters.sortedForReading(entry, memberIds)

    override fun sortedForDisplay(
        entry: Entry,
        chapters: List<EntryChapter>,
        memberIds: List<Long>,
    ): List<EntryChapter> = chapters.sortedForMergedDisplay(entry, memberIds)

    override fun progressLabels(request: EntryChildProgressRequest): Flow<Map<Long, EntryChildProgressLabel>> {
        val stateFlows = request.memberIds.distinct().map(entryProgressRepository::getByEntryIdAsFlow)
        if (stateFlows.isEmpty()) return flowOf(emptyMap())

        return combine(stateFlows) { statesByMember ->
            val progressByChapterId = statesByMember.flatMap { it }.associateBy { it.chapterId }
            request.chapters.mapNotNull { chapter ->
                if (chapter.read) return@mapNotNull null
                val progress = progressByChapterId[chapter.id] ?: return@mapNotNull null
                if (!progress.hasPartialBookProgress) return@mapNotNull null
                chapter.id to EntryChildProgressLabel(MR.strings.label_started)
            }.toMap()
        }
    }
}
