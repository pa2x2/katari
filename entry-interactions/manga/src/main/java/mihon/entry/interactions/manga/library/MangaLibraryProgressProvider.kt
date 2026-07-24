package mihon.entry.interactions.manga

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryLibraryProgressEvidence
import mihon.entry.interactions.EntryLibraryProgressProvider
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryProgressRepository

internal class MangaLibraryProgressProvider(
    private val entryProgressRepository: EntryProgressRepository,
) : EntryLibraryProgressProvider {
    override val type = EntryType.MANGA

    override suspend fun evidence(entry: Entry, chapters: List<EntryChapter>): EntryLibraryProgressEvidence {
        entry.requireManga()
        val chapterIds = chapters.mapTo(mutableSetOf(), EntryChapter::id)
        val progress = chapters
            .map(EntryChapter::entryId)
            .distinct()
            .flatMap { entryId -> entryProgressRepository.getByEntryId(entryId) }
            .filter { it.chapterId in chapterIds }
        val inProgress = progress
            .filter { !it.completed && it.pageIndex > 0L }
            .maxByOrNull { it.lastReadAt }

        return EntryLibraryProgressEvidence(
            hasMediaProgress = progress.any { it.pageIndex > 0L },
            inProgressItemId = inProgress?.chapterId,
            inProgressFraction = inProgress?.locator?.progression?.toFloat(),
            lastActivityAt = progress.maxOfOrNull { it.lastReadAt } ?: 0L,
        )
    }
}
