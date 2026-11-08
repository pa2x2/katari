package tachiyomi.domain.history.model

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.EntryCover
import java.util.Date

data class HistoryWithRelations(
    val id: Long,
    val chapterId: Long,
    val entryId: Long,
    val entryType: EntryType,
    val title: String,
    val chapterName: String,
    val chapterNumber: Double,
    /** Title of the group the chapter belongs to, such as a volume; null when it belongs to none. */
    val chapterGroupTitle: String? = null,
    val readAt: Date?,
    val readDuration: Long,
    val coverData: EntryCover,
)
