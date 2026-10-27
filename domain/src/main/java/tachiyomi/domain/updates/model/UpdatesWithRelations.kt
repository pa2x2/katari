package tachiyomi.domain.updates.model

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.EntryCover

data class UpdatesWithRelations(
    val entryId: Long,
    val entryType: EntryType,
    val entryTitle: String,
    /** The name the entry's downloads are stored under, which differs from [entryTitle] for a merged entry's member. */
    val entryDownloadTitle: String,
    val chapterId: Long,
    val chapterName: String,
    val scanlator: String?,
    /** Title of the group the chapter belongs to, such as a volume; null when it belongs to none. */
    val chapterGroupTitle: String? = null,
    val chapterUrl: String,
    val read: Boolean,
    val bookmark: Boolean,
    val started: Boolean,
    val progressPosition: Long,
    val sourceId: Long,
    val dateFetch: Long,
    val coverData: EntryCover,
    val dateUpload: Long = 0,
)
