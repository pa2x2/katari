package tachiyomi.domain.entry.model

import kotlinx.serialization.json.JsonObject
import mihon.core.common.extensions.EMPTY

data class EntryChapter(
    val id: Long,
    val entryId: Long,
    val url: String,
    val name: String,
    val read: Boolean,
    val bookmark: Boolean,
    val lastPageRead: Long,
    val chapterNumber: Double,
    val scanlator: String?,
    val dateUpload: Long,
    val dateFetch: Long,
    val sourceOrder: Long,
    val lastModifiedAt: Long,
    val version: Long,
    val isSyncing: Boolean,
    val memo: JsonObject,
) {
    val isRecognizedNumber: Boolean
        get() = chapterNumber >= 0f

    fun copyFrom(other: EntryChapter): EntryChapter {
        return copy(
            name = other.name,
            url = other.url,
            dateUpload = other.dateUpload,
            chapterNumber = other.chapterNumber,
            scanlator = other.scanlator?.ifBlank { null },
            memo = other.memo,
        )
    }

    companion object {
        fun create() = EntryChapter(
            id = -1,
            entryId = -1,
            url = "",
            name = "",
            read = false,
            bookmark = false,
            lastPageRead = 0,
            chapterNumber = -1.0,
            scanlator = null,
            dateUpload = -1,
            dateFetch = 0,
            sourceOrder = 0,
            lastModifiedAt = 0,
            version = 1,
            isSyncing = false,
            memo = JsonObject.EMPTY,
        )
    }
}
