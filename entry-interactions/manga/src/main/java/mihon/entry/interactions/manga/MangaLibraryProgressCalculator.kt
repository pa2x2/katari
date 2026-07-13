package mihon.entry.interactions.manga

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.service.EntryLibraryProgressCalculator
import tachiyomi.domain.entry.service.EntryLibraryState
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.ProgressState

fun mangaEntryLibraryProgressCalculator(
    entryProgressRepository: EntryProgressRepository,
): EntryLibraryProgressCalculator {
    return MangaLibraryProgressCalculator(entryProgressRepository)
}

private class MangaLibraryProgressCalculator(
    private val entryProgressRepository: EntryProgressRepository,
) : EntryLibraryProgressCalculator {
    override val entryType = EntryType.MANGA

    override suspend fun calculate(
        entry: Entry,
        chapters: List<EntryChapter>,
        lastRead: Long,
    ): EntryLibraryState {
        entry.requireManga()
        val chapterIds = chapters.mapTo(mutableSetOf(), EntryChapter::id)
        val progress = chapters
            .map { it.entryId }
            .distinct()
            .flatMap { entryId -> entryProgressRepository.getByEntryId(entryId) }
            .filter { it.chapterId in chapterIds }
        val inProgress = progress
            .filter { !it.completed && it.pageIndex > 0L }
            .maxByOrNull { it.lastReadAt }

        return EntryLibraryState(
            progress = ProgressState(
                totalCount = chapters.size.toLong(),
                consumedCount = chapters.count { it.read }.toLong(),
                bookmarkCount = chapters.count { it.bookmark }.toLong(),
                inProgressItemId = inProgress?.chapterId,
                inProgressFraction = inProgress?.locator?.progression?.toFloat(),
                hasStarted = chapters.any { it.read } || progress.any { it.pageIndex > 0L },
                continueMode = ProgressState.ContinueMode.TARGET_AVAILABLE,
            ),
            lastRead = maxOf(lastRead, progress.maxOfOrNull { it.lastReadAt } ?: 0L),
            continueEntryId = inProgress?.chapterId ?: chapters.firstOrNull { !it.read }?.id,
        )
    }

    override fun merge(members: List<LibraryItem>): EntryLibraryState {
        return EntryLibraryState(
            progress = ProgressState(
                totalCount = members.sumOf { it.totalCount },
                consumedCount = members.sumOf { it.consumedCount },
                bookmarkCount = members.sumOf { it.progress.bookmarkCount },
                inProgressItemId = members.firstNotNullOfOrNull { it.progress.inProgressItemId },
                inProgressFraction = members.firstNotNullOfOrNull { it.progress.inProgressFraction },
                hasStarted = members.any { it.hasStarted },
                continueMode = ProgressState.ContinueMode.TARGET_AVAILABLE,
            ),
            lastRead = members.maxOfOrNull { it.lastRead } ?: 0L,
            continueEntryId = members.firstNotNullOfOrNull { it.continueEntryId },
        )
    }
}
