package mihon.entry.interactions.book

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.service.EntryLibraryProgressCalculator
import tachiyomi.domain.entry.service.EntryLibraryState
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.ProgressState

fun bookEntryLibraryProgressCalculator(
    entryProgressRepository: EntryProgressRepository,
): EntryLibraryProgressCalculator = BookLibraryProgressCalculator(entryProgressRepository)

private class BookLibraryProgressCalculator(
    private val entryProgressRepository: EntryProgressRepository,
) : EntryLibraryProgressCalculator {
    override val entryType = EntryType.BOOK

    override suspend fun calculate(
        entry: Entry,
        chapters: List<EntryChapter>,
        lastRead: Long,
    ): EntryLibraryState {
        entry.requireBook()
        val chapterIds = chapters.mapTo(mutableSetOf(), EntryChapter::id)
        val states = chapters
            .map(EntryChapter::entryId)
            .distinct()
            .flatMap { entryProgressRepository.getByEntryId(it) }
            .filter { it.chapterId in chapterIds }
        val inProgress = states
            .filter { !it.completed && !it.locator.isEmpty }
            .maxByOrNull { it.locatorUpdatedAt }

        return EntryLibraryState(
            progress = ProgressState(
                totalCount = chapters.size.toLong(),
                consumedCount = chapters.count { it.read }.toLong(),
                bookmarkCount = 0,
                inProgressItemId = inProgress?.chapterId,
                inProgressFraction = inProgress?.locator?.let { locator ->
                    (locator.totalProgression ?: locator.progression)?.toFloat()
                },
                hasStarted = chapters.any { it.read } || states.any { !it.locator.isEmpty },
                continueMode = ProgressState.ContinueMode.TARGET_AVAILABLE,
            ),
            lastRead = maxOf(lastRead, states.maxOfOrNull { it.locatorUpdatedAt } ?: 0L),
            continueEntryId = inProgress?.chapterId ?: chapters.firstOrNull { !it.read }?.id,
        )
    }

    override fun merge(members: List<LibraryItem>): EntryLibraryState {
        val progress = ProgressState(
            totalCount = members.sumOf { it.totalCount },
            consumedCount = members.sumOf { it.consumedCount },
            bookmarkCount = 0,
            inProgressItemId = members.firstNotNullOfOrNull { it.progress.inProgressItemId },
            inProgressFraction = members.firstNotNullOfOrNull { it.progress.inProgressFraction },
            hasStarted = members.any { it.hasStarted },
            continueMode = ProgressState.ContinueMode.TARGET_AVAILABLE,
        )
        return EntryLibraryState(
            progress = progress,
            lastRead = members.maxOfOrNull { it.lastRead } ?: 0L,
            continueEntryId = progress.inProgressItemId ?: members.firstNotNullOfOrNull { it.continueEntryId },
        )
    }
}
