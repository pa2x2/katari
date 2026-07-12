package eu.kanade.tachiyomi.source.entry

import kotlinx.serialization.json.JsonObject

/**
 * Source-level representation of a chapter.
 */
interface SEntryChapter {

    var url: String

    var name: String

    var dateUpload: Long

    var chapterNumber: Double

    var scanlator: String?

    /**
     * Extra metadata associated with the chapter.
     *
     * The JSON object is not visible to users and is intended for internal or
     * source-specific purposes. Apps may define their own namespaced keys
     * (e.g. `"mihon.*"`) for sources to populate.
     */
    var memo: JsonObject

    fun copyFrom(other: SEntryChapter) {
        name = other.name
        url = other.url
        dateUpload = other.dateUpload
        chapterNumber = other.chapterNumber
        scanlator = other.scanlator
        memo = other.memo
    }

    fun copy() = create().also {
        it.url = url
        it.name = name
        it.dateUpload = dateUpload
        it.chapterNumber = chapterNumber
        it.scanlator = scanlator
        it.memo = memo
    }

    companion object {
        fun create(): SEntryChapter = SEntryChapterImpl()
    }
}
