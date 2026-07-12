package mihon.entry.interactions.manga

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.service.EntryLibraryProgressCalculator
import tachiyomi.domain.entry.service.EntryLibraryState
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.ProgressState

fun mangaEntryLibraryProgressCalculator(): EntryLibraryProgressCalculator {
    return MangaLibraryProgressCalculator
}

private object MangaLibraryProgressCalculator : EntryLibraryProgressCalculator {
    override val entryType = EntryType.MANGA

    override suspend fun calculate(
        entry: Entry,
        chapters: List<EntryChapter>,
        lastRead: Long,
    ): EntryLibraryState {
        entry.requireManga()

        return EntryLibraryState(
            progress = ProgressState(
                totalCount = chapters.size.toLong(),
                consumedCount = chapters.count { it.read }.toLong(),
                bookmarkCount = chapters.count { it.bookmark }.toLong(),
                hasStarted = chapters.any { it.read || it.lastPageRead > 0 },
            ),
            lastRead = lastRead,
            continueEntryId = null,
        )
    }

    override fun merge(members: List<LibraryItem>): EntryLibraryState {
        return EntryLibraryState(
            progress = ProgressState(
                totalCount = members.sumOf { it.totalCount },
                consumedCount = members.sumOf { it.consumedCount },
                bookmarkCount = members.sumOf { it.progress.bookmarkCount },
                hasStarted = members.any { it.hasStarted },
            ),
            lastRead = members.maxOfOrNull { it.lastRead } ?: 0L,
            continueEntryId = null,
        )
    }
}
