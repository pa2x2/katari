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

    /**
     * The release time as the site shows it, such as "3 days ago" or "Spring 2024", for a chapter whose time can't be
     * given as [dateUpload]. Shown in place of the date; leave it null whenever [dateUpload] is set.
     */
    var dateText: String?

    /** Parsed child number, or `-1.0` when unknown. */
    var chapterNumber: Double

    /** Scanlator, release group, or another provider attribution, when known. */
    var scanlator: String?

    /** Short synopsis of this chapter or episode, when the source has one. */
    var summary: String?

    /** Still or preview image URL for this chapter or episode, loaded with the source's headers. */
    var previewUrl: String?

    /** Whether this chapter belongs to the main story; [ChapterKind.REGULAR] unless the source knows otherwise. */
    var kind: ChapterKind

    /**
     * Key of the [SEntryChapterGroup] in [EntryRefresh.chapterGroups] this chapter belongs to, or null when it belongs
     * to none. A key the refresh doesn't declare counts as none.
     */
    var group: String?

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
        summary = other.summary
        previewUrl = other.previewUrl
        kind = other.kind
        group = other.group
        dateText = other.dateText
        memo = other.memo
    }

    /** Creates a detached mutable copy preserving all current fields. */
    fun copy() = create().also {
        it.url = url
        it.name = name
        it.dateUpload = dateUpload
        it.chapterNumber = chapterNumber
        it.scanlator = scanlator
        it.summary = summary
        it.previewUrl = previewUrl
        it.kind = kind
        it.group = group
        it.dateText = dateText
        it.memo = memo
    }

    /** Factory for [SEntryChapter]. */
    companion object {
        /** Creates an empty mutable source child item. */
        fun create(): SEntryChapter = SEntryChapterImpl()
    }
}
