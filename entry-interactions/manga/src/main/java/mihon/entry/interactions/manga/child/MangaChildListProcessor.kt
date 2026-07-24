package mihon.entry.interactions.manga

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import mihon.entry.interactions.EntryChildListDisplay
import mihon.entry.interactions.EntryChildListProcessor
import mihon.entry.interactions.EntryChildListRequest
import mihon.entry.interactions.EntryChildListRow
import mihon.entry.interactions.EntryChildProgressLabel
import mihon.entry.interactions.EntryChildProgressProcessor
import mihon.entry.interactions.EntryChildProgressRequest
import mihon.entry.interactions.EntryMissingChildGapProcessor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.service.calculateChapterGap
import tachiyomi.domain.entry.service.missingChaptersCount
import tachiyomi.domain.entry.service.sortedForMergedDisplay
import tachiyomi.domain.entry.service.sortedForReading
import tachiyomi.i18n.MR
import kotlin.math.floor

internal class MangaChildListProcessor(
    private val entryProgressRepository: EntryProgressRepository,
) : EntryChildListProcessor, EntryChildProgressProcessor, EntryMissingChildGapProcessor {
    override val type: EntryType = EntryType.MANGA

    override fun sortedForReading(
        entry: Entry,
        chapters: List<EntryChapter>,
        memberIds: List<Long>,
    ): List<EntryChapter> {
        return chapters.sortedForReading(entry, memberIds)
    }

    override fun sortedForDisplay(
        entry: Entry,
        chapters: List<EntryChapter>,
        memberIds: List<Long>,
    ): List<EntryChapter> {
        return chapters.sortedForMergedDisplay(entry, memberIds)
    }

    override fun buildDisplayList(request: EntryChildListRequest): EntryChildListDisplay {
        val sortedChapters = sortedForDisplay(
            entry = request.entry,
            chapters = request.chapters,
            memberIds = request.memberIds,
        )
        val rows = if (request.memberIds.size <= 1) {
            sortedChapters.withMissingChapterCounts(request.entry, request.includeMissingCounts)
        } else {
            buildList {
                request.memberIds.forEach { memberId ->
                    val memberChapters = sortedChapters.filter { it.entryId == memberId }
                    if (memberChapters.isEmpty()) return@forEach

                    add(
                        EntryChildListRow.MemberHeader(
                            entryId = memberId,
                            title = request.memberTitleById[memberId].orEmpty().ifBlank { request.fallbackTitle },
                        ),
                    )
                    addAll(memberChapters.withMissingChapterCounts(request.entry, request.includeMissingCounts))
                }
            }
        }
        return EntryChildListDisplay(
            rows = rows,
            aggregateMissingCount = request.chapters.map(EntryChapter::chapterNumber).missingChaptersCount(),
        )
    }

    override fun progressLabels(request: EntryChildProgressRequest): Flow<Map<Long, EntryChildProgressLabel>> {
        val stateFlows = request.memberIds
            .distinct()
            .map(entryProgressRepository::getByEntryIdAsFlow)
        if (stateFlows.isEmpty()) return flowOf(emptyMap())

        return combine(stateFlows) { statesByMember ->
            val progressByChapterId = statesByMember.flatMap { it }.associateBy { it.chapterId }
            request.chapters.mapNotNull { chapter ->
                if (chapter.read) return@mapNotNull null
                val progress = progressByChapterId[chapter.id] ?: return@mapNotNull null
                if (!progress.hasPartialMangaProgress) return@mapNotNull null
                chapter.id to EntryChildProgressLabel(
                    resource = MR.strings.chapter_progress,
                    args = listOf(progress.pageIndex + 1),
                )
            }.toMap()
        }
    }

    private fun List<EntryChapter>.withMissingChapterCounts(
        entry: Entry,
        includeMissingCounts: Boolean,
    ): List<EntryChildListRow> {
        if (!includeMissingCounts) {
            return map(EntryChildListRow::Child)
        }

        return buildList<EntryChildListRow> {
            for (index in this@withMissingChapterCounts.indices) {
                val before = this@withMissingChapterCounts.getOrNull(index - 1)
                val after = this@withMissingChapterCounts[index]
                missingCountRow(entry, before, after)?.let(::add)
                add(EntryChildListRow.Child(after))
            }
        }
    }

    private fun missingCountRow(
        entry: Entry,
        before: EntryChapter?,
        after: EntryChapter?,
    ): EntryChildListRow.MissingCount? {
        val (lowerChapter, higherChapter) = if (entry.sortDescending()) {
            after to before
        } else {
            before to after
        }
        if (higherChapter == null) return null

        val missingCount = if (lowerChapter == null) {
            floor(higherChapter.chapterNumber)
                .toInt()
                .minus(1)
                .coerceAtLeast(0)
        } else {
            calculateChapterGap(higherChapter, lowerChapter)
        }
        return missingCount
            .takeIf { it > 0 }
            ?.let {
                EntryChildListRow.MissingCount(
                    id = "${lowerChapter?.id}-${higherChapter.id}",
                    count = it,
                )
            }
    }
}
