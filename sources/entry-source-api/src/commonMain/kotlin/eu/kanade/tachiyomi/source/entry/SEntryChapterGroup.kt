package eu.kanade.tachiyomi.source.entry

/**
 * A part of an entry's story that some of its chapters belong to, such as a volume, an arc, a book or a season whose
 * episodes the site lists on the entry's own page. The host shows each group as a heading over its chapters and reads
 * the groups in the order the source lists them.
 *
 * A group is not a copy made by someone else: a translation team, branch or dub is the chapter's
 * [SEntryChapter.scanlator]. A part with a page and details of its own is an entry of its own, listed through
 * [MergedEntrySource].
 */
interface SEntryChapterGroup {

    /** Identifies the group within its entry; chapters name it in [SEntryChapter.group]. Keep it when [title] changes. */
    var key: String

    /** User-visible heading, such as "Volume 3". */
    var title: String

    /** Factory for [SEntryChapterGroup]. */
    companion object {
        /** Creates a group with [key] and [title]. */
        fun create(key: String, title: String): SEntryChapterGroup = SEntryChapterGroupImpl().also {
            it.key = key
            it.title = title
        }
    }
}
