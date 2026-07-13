package eu.kanade.tachiyomi.source.entry

import kotlinx.serialization.json.JsonObject

/**
 * Source-level representation of a chapter.
 */
interface SEntryChapter {

    /** Stable child-item identity within the source. */
    var url: String

    /** User-visible child-item label. */
    var name: String

    /** Upload timestamp in Unix epoch milliseconds, or zero when unknown. */
    var dateUpload: Long

    /** Parsed child number, or `-1.0` when unknown. */
    var chapterNumber: Double

    /** Scanlator, release group, or another provider attribution, when known. */
    var scanlator: String?

    /**
     * Extra metadata associated with the chapter.
     *
     * The JSON object is not visible to users and is intended for internal or
     * source-specific purposes. Apps may define their own namespaced keys
     * (e.g. `"mihon.*"`) for sources to populate.
     */
    var memo: JsonObject

    /** Copies every public field from [other] into this instance. */
    fun copyFrom(other: SEntryChapter) {
        name = other.name
        url = other.url
        dateUpload = other.dateUpload
        chapterNumber = other.chapterNumber
        scanlator = other.scanlator
        memo = other.memo
    }

    /** Creates a detached mutable copy preserving all current fields. */
    fun copy() = create().also {
        it.url = url
        it.name = name
        it.dateUpload = dateUpload
        it.chapterNumber = chapterNumber
        it.scanlator = scanlator
        it.memo = memo
    }

    /** Factory for [SEntryChapter]. */
    companion object {
        /** Creates an empty mutable source child item. */
        fun create(): SEntryChapter = SEntryChapterImpl()
    }
}
