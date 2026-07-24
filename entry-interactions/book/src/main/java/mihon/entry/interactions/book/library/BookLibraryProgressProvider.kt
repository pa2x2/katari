package mihon.entry.interactions.book

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryLibraryProgressEvidence
import mihon.entry.interactions.EntryLibraryProgressProvider
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryProgressRepository

internal class BookLibraryProgressProvider(
    private val entryProgressRepository: EntryProgressRepository,
) : EntryLibraryProgressProvider {
    override val type = EntryType.BOOK

    override suspend fun evidence(entry: Entry, chapters: List<EntryChapter>): EntryLibraryProgressEvidence {
        entry.requireBook()
        val chapterIds = chapters.mapTo(mutableSetOf(), EntryChapter::id)
        val states = chapters
            .map(EntryChapter::entryId)
            .distinct()
            .flatMap { entryId -> entryProgressRepository.getByEntryId(entryId) }
            .filter { it.chapterId in chapterIds }
        val inProgress = states
            .filter { !it.completed && !it.locator.isEmpty }
            .maxByOrNull { it.locatorUpdatedAt }

        return EntryLibraryProgressEvidence(
            hasMediaProgress = states.any { !it.locator.isEmpty },
            inProgressItemId = inProgress?.chapterId,
            inProgressFraction = inProgress?.locator?.let { locator ->
                (locator.totalProgression ?: locator.progression)?.toFloat()
            },
            lastActivityAt = states.maxOfOrNull { it.locatorUpdatedAt } ?: 0L,
        )
    }
}
