package tachiyomi.data.history

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.domain.history.model.History
import tachiyomi.domain.history.model.HistoryWithRelations
import java.util.Date

object HistoryMapper {
    fun mapHistory(
        id: Long,
        chapterId: Long,
        readAt: Date?,
        readDuration: Long,
    ): History = History(
        id = id,
        chapterId = chapterId,
        readAt = readAt,
        readDuration = readDuration,
    )

    fun mapHistoryWithRelations(
        historyId: Long,
        entryId: Long,
        entryType: String,
        chapterId: Long,
        title: String,
        thumbnailUrl: String?,
        source: Long,
        favorite: Boolean,
        coverLastModified: Long,
        chapterName: String,
        chapterNumber: Double,
        readAt: Date?,
        readDuration: Long,
    ): HistoryWithRelations = HistoryWithRelations(
        id = historyId,
        chapterId = chapterId,
        entryId = entryId,
        entryType = EntryType.valueOf(entryType.uppercase()),
        title = title,
        chapterName = chapterName,
        chapterNumber = chapterNumber,
        readAt = readAt,
        readDuration = readDuration,
        coverData = EntryCover(
            entryId = entryId,
            sourceId = source,
            isFavorite = favorite,
            url = thumbnailUrl,
            lastModified = coverLastModified,
        ),
    )
}
