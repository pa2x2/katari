package mihon.entry.interactions.book.reader

import kotlinx.coroutines.CancellationException
import mihon.entry.interactions.book.migration.isPendingBookMigration
import mihon.entry.interactions.book.preparation.PreparedBookPublication
import mihon.entry.interactions.book.state.BOOK_PROGRESS_LOCATOR_KIND
import mihon.entry.interactions.book.state.BookProgressIdentity
import mihon.entry.interactions.book.state.BookProgressLocatorCodec
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.repository.EntryProgressRepository

/** Promotes reconciled migration evidence without discarding positions the current publication cannot map. */
internal class BookReaderProgressResolver(private val repository: EntryProgressRepository) {
    suspend fun resolve(
        chapter: EntryChapter,
        progressIdentity: BookProgressIdentity,
        preparedPublication: PreparedBookPublication,
    ): EntryProgressState? {
        var current = repository.get(
            chapter.entryId,
            progressIdentity.contentKey,
            progressIdentity.resourceKey,
        )
        val pendingStates = repository.getByEntryId(chapter.entryId)
            .filter { state ->
                state.chapterId == chapter.id && state.isPendingBookMigration
            }
            .sortedByDescending { maxOf(it.locatorUpdatedAt, it.completionUpdatedAt) }
        pendingStates.forEach { pending ->
            val currentLocator = current
                ?.locator
                ?.let(BookProgressLocatorCodec::decode)
                ?.takeIf(preparedPublication::validate)
            val migratedLocator = if (currentLocator == null) {
                val sourceLocator = BookProgressLocatorCodec.decode(pending.locator)
                try {
                    sourceLocator
                        ?.let { preparedPublication.reconcileMigratedLocator(it) }
                        ?.takeIf(preparedPublication::validate)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    return current
                }
            } else {
                null
            }
            // A target location can supersede pending evidence; an unresolved partial location cannot.
            if (currentLocator == null && migratedLocator == null && !pending.completed) return@forEach
            val resolvedPending = pending.copy(
                resourceRevision = progressIdentity.resourceRevision,
                locator = migratedLocator?.let { locator ->
                    val progression = preparedPublication.progression(locator)
                    BookProgressLocatorCodec.encode(
                        locator.copy(totalProgression = progression),
                        pending.locator.extensions,
                        publicationProgression = progression,
                    )
                } ?: EntryProgressLocator(kind = BOOK_PROGRESS_LOCATOR_KIND),
                locatorUpdatedAt = if (migratedLocator == null && current != null) {
                    0L
                } else {
                    pending.locatorUpdatedAt
                },
            )
            repository.upsert(resolvedPending)
            repository.rekey(
                entryId = chapter.entryId,
                chapterId = chapter.id,
                oldContentKey = resolvedPending.contentKey,
                oldResourceKey = resolvedPending.resourceKey,
                newContentKey = progressIdentity.contentKey,
                newResourceKey = progressIdentity.resourceKey,
            )
            current = repository.get(
                chapter.entryId,
                progressIdentity.contentKey,
                progressIdentity.resourceKey,
            )
        }
        return current
    }
}
