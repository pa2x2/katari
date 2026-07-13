package eu.kanade.tachiyomi.source.entry

import kotlinx.serialization.json.JsonObject

/**
 * Source-level representation of a content item.
 *
 * Sources may return items of any [EntryType] in the same list.
 */
interface SEntry {

    /** Stable entry identity within this source. */
    var url: String

    /** User-visible entry title. */
    var title: String

    /** Artist or illustrator name, when known. */
    var artist: String?

    /** Author or creator name, when known. */
    var author: String?

    /** User-visible entry description, when known. */
    var description: String?

    /** User-visible genres or tags, when known. */
    var genre: List<String>?

    /** Publication status represented by one of the companion status constants. */
    var status: Int

    /** Cover or catalogue thumbnail URL, when available. */
    var thumbnailUrl: String?

    /** Optional strategy controlling future library metadata updates. */
    var updateStrategy: EntryUpdateStrategy?

    /** Whether the details request has initialized this entry. */
    var initialized: Boolean

    /**
     * Extra metadata associated with the entry.
     *
     * The JSON object is not visible to users and is intended for internal or
     * source-specific purposes. Apps may define their own namespaced keys
     * (e.g. `"mihon.*"`) for sources to populate.
     */
    var memo: JsonObject

    /** Content type that determines the applicable Katari interactions and renderer. */
    var type: EntryType

    /** Creates a detached mutable copy preserving all current fields. */
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

    /** Factory and publication-status constants for [SEntry]. */
    companion object {
        /** Publication status is unknown. */
        const val UNKNOWN = 0

        /** Entry is currently being published. */
        const val ONGOING = 1

        /** Entry has completed publication. */
        const val COMPLETED = 2

        /** Entry is licensed and may no longer be available from the source. */
        const val LICENSED = 3

        /** Publication finished without another more specific status. */
        const val PUBLISHING_FINISHED = 4

        /** Entry publication was cancelled. */
        const val CANCELLED = 5

        /** Entry publication is temporarily on hiatus. */
        const val ON_HIATUS = 6

        /** Creates an empty mutable source entry. */
        fun create(): SEntry = SEntryImpl()
    }
}
