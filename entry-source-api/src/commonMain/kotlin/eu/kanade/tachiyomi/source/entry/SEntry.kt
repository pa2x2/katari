package eu.kanade.tachiyomi.source.entry

import kotlinx.serialization.json.JsonObject

/**
 * Source-level representation of a content item.
 *
 * Sources may return items of any [EntryType] in the same list.
 */
interface SEntry {

    var url: String

    var title: String

    var artist: String?

    var author: String?

    var description: String?

    var genre: List<String>?

    var status: Int

    var thumbnailUrl: String?

    var updateStrategy: EntryUpdateStrategy?

    var initialized: Boolean

    /**
     * Extra metadata associated with the entry.
     *
     * The JSON object is not visible to users and is intended for internal or
     * source-specific purposes. Apps may define their own namespaced keys
     * (e.g. `"mihon.*"`) for sources to populate.
     */
    var memo: JsonObject

    var type: EntryType

    fun copy() = create().also {
        it.url = url
        it.title = title
        it.artist = artist
        it.author = author
        it.description = description
        it.genre = genre?.toList()
        it.status = status
        it.thumbnailUrl = thumbnailUrl
        it.updateStrategy = updateStrategy
        it.initialized = initialized
        it.memo = memo
        it.type = type
    }

    companion object {
        const val UNKNOWN = 0
        const val ONGOING = 1
        const val COMPLETED = 2
        const val LICENSED = 3
        const val PUBLISHING_FINISHED = 4
        const val CANCELLED = 5
        const val ON_HIATUS = 6

        fun create(): SEntry = SEntryImpl()
    }
}
