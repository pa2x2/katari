package tachiyomi.source.local

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.EntryUpdateStrategy
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import kotlinx.serialization.json.JsonObject
import mihon.core.common.extensions.EMPTY

internal data class LocalEntryMetadata(
    var url: String,
    var title: String,
    var thumbnailUrl: String? = null,
    var artist: String? = null,
    var author: String? = null,
    var status: Int = UNKNOWN,
    var description: String? = null,
    var genre: List<String>? = null,
    var updateStrategy: EntryUpdateStrategy = EntryUpdateStrategy.ALWAYS_UPDATE,
    var initialized: Boolean = false,
    var memo: JsonObject = JsonObject.EMPTY,
) {
    fun toSEntry(): SEntry = SEntry.create().also {
        it.url = url
        it.title = title
        it.artist = artist
        it.author = author
        it.description = description
        it.genre = genre
        it.status = status
        it.thumbnailUrl = thumbnailUrl
        it.updateStrategy = updateStrategy
        it.initialized = initialized
        it.memo = memo
        it.type = EntryType.MANGA
    }

    companion object {
        const val UNKNOWN = 0
        const val ONGOING = 1
        const val COMPLETED = 2
        const val LICENSED = 3
        const val PUBLISHING_FINISHED = 4
        const val CANCELLED = 5
        const val ON_HIATUS = 6
    }
}

internal fun SEntry.toLocalEntryMetadata(): LocalEntryMetadata {
    require(type == EntryType.MANGA) { "Local source only supports manga entries" }
    return LocalEntryMetadata(
        url = url,
        title = title,
        thumbnailUrl = thumbnailUrl,
        artist = artist,
        author = author,
        status = status,
        description = description,
        genre = genre,
        updateStrategy = updateStrategy ?: EntryUpdateStrategy.ALWAYS_UPDATE,
        initialized = initialized,
        memo = memo,
    )
}

internal data class LocalEntryChapterMetadata(
    var url: String,
    var name: String,
    var chapterNumber: Double = -1.0,
    var scanlator: String? = null,
    var dateUpload: Long = 0,
    var memo: JsonObject = JsonObject.EMPTY,
) {
    fun toSEntryChapter(): SEntryChapter = SEntryChapter.create().also {
        it.url = url
        it.name = name
        it.dateUpload = dateUpload
        it.chapterNumber = chapterNumber
        it.scanlator = scanlator
        it.memo = memo
    }
}
