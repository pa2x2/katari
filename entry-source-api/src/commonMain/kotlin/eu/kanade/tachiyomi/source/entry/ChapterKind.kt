package eu.kanade.tachiyomi.source.entry

/**
 * What a chapter or episode is to the entry's story.
 *
 * [code] is the stored and backed-up form; a kind keeps its code when new kinds are added.
 */
enum class ChapterKind(val code: Int) {
    /** Part of the main story. */
    REGULAR(0),

    /** Outside the main story: an anime filler episode, an omake or side chapter. */
    FILLER(1),
    ;

    /** Decoding of stored codes. */
    companion object {
        /**
         * Returns the kind stored as [code]. A code written by a newer version reads as [REGULAR], so its
         * databases and backups still load.
         */
        fun fromCode(code: Int): ChapterKind = entries.find { it.code == code } ?: REGULAR
    }
}
