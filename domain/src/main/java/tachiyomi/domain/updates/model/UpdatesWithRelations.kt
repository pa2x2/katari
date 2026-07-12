package tachiyomi.domain.updates.model

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.EntryCover

data class UpdatesWithRelations(
    val entryId: Long,
    val entryType: EntryType,
    val entryTitle: String,
    val chapterId: Long,
    val chapterName: String,
    val scanlator: String?,
    val chapterUrl: String,
    val read: Boolean,
    val bookmark: Boolean,
    val lastPageRead: Long,
    val sourceId: Long,
    val dateFetch: Long,
    val coverData: EntryCover,
    val dateUpload: Long = 0,
    val excludedScanlator: String? = null,
)
