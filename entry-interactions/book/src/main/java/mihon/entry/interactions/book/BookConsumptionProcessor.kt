package mihon.entry.interactions.book

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryConsumptionProcessor
import mihon.entry.interactions.consumptionStatus
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.repository.EntryProgressRepository

internal class BookConsumptionProcessor(
    private val entryProgressRepository: EntryProgressRepository,
    private val identityResolver: BookProgressIdentityResolver,
    private val now: () -> Long = System::currentTimeMillis,
) : EntryConsumptionProcessor {
    override val type = EntryType.BOOK
    override val supportsBookmark = false

    override suspend fun setConsumed(entry: Entry, chapters: List<EntryChapter>, consumed: Boolean) {
        entry.requireBook()
        val chaptersToUpdate = chapters.filter { canSetConsumed(it.consumptionStatus(), consumed) }
        if (chaptersToUpdate.isEmpty()) return

        val timestamp = now()
        chaptersToUpdate.forEach { chapter ->
            val identity = identityResolver.resolve(chapter)
            val current = entryProgressRepository.get(
                entryId = chapter.entryId,
                contentKey = identity.contentKey,
                resourceKey = identity.resourceKey,
            )
            val updated = current?.copy(
                chapterId = chapter.id,
                completed = consumed,
                completionUpdatedAt = timestamp,
            ) ?: EntryProgressState(
                entryId = chapter.entryId,
                chapterId = chapter.id,
                contentKey = identity.contentKey,
                resourceKey = identity.resourceKey,
                resourceRevision = identity.resourceRevision,
                locator = EntryProgressLocator(kind = BOOK_PROGRESS_LOCATOR_KIND),
                completed = consumed,
                completionUpdatedAt = timestamp,
            )
            entryProgressRepository.mergeAndSyncChild(updated)
        }
    }

    override suspend fun setBookmarked(entry: Entry, chapters: List<EntryChapter>, bookmarked: Boolean) {
        entry.requireBook()
    }
}
