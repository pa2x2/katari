package tachiyomi.domain.history.model

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.domain.library.model.LibraryItemKey
import java.time.Instant

/**
 * Unified history item. Manga chapter reads and anime episode watches share common fields;
 * type-specific details are kept in the subclasses.
 */
sealed class HistoryItem {
    abstract val history: HistoryWithRelations
    abstract val key: LibraryItemKey
    abstract val entryId: Long
    abstract val entryType: EntryType
    abstract val entryTitle: String
    abstract val coverData: EntryCover
    abstract val sourceId: Long
    abstract val readAt: Instant
    abstract val duration: Long

    data class EntryHistory(override val history: HistoryWithRelations) : HistoryItem() {
        override val key = LibraryItemKey(history.entryType, history.chapterId)
        override val entryId = history.entryId
        override val entryType = history.entryType
        override val entryTitle = history.title
        override val coverData = history.coverData
        override val sourceId = history.coverData.sourceId
        override val readAt = history.readAt?.toInstant() ?: Instant.EPOCH
        override val duration = history.readDuration
    }
}

fun HistoryWithRelations.toHistoryItem(): HistoryItem {
    return HistoryItem.EntryHistory(this)
}
