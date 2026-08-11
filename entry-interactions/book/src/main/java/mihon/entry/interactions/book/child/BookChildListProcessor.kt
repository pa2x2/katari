package mihon.entry.interactions.book.child

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import mihon.entry.interactions.book.state.hasPartialBookProgress
import mihon.entry.interactions.child.EntryChildProgressLabel
import mihon.entry.interactions.child.EntryChildProgressRequest
import mihon.entry.interactions.runtime.EntryChildListProcessor
import mihon.entry.interactions.runtime.EntryChildProgressProcessor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.service.sortedForMergedDisplay
import tachiyomi.domain.entry.service.sortedForReading
import tachiyomi.i18n.MR
import kotlin.math.roundToInt

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
            val progressByChapterId = statesByMember
                .flatMap { it }
                .filter { it.chapterId != null }
                .groupBy { it.chapterId }
                .mapValues { (_, states) -> states.maxBy { it.locatorUpdatedAt } }
            request.chapters.mapNotNull { chapter ->
                if (chapter.read) return@mapNotNull null
                val progress = progressByChapterId[chapter.id] ?: return@mapNotNull null
                if (!progress.hasPartialBookProgress) return@mapNotNull null
                val label = progress.locator.progression?.let { progression ->
                    EntryChildProgressLabel(
                        resource = MR.strings.book_chapter_progress,
                        args = listOf((progression * 100).roundToInt().coerceIn(0, 100)),
                    )
                } ?: EntryChildProgressLabel(MR.strings.label_started)
                chapter.id to label
            }.toMap()
        }
    }
}
