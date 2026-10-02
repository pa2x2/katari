package tachiyomi.domain.library.update.model

/** How library updates treat one entry, chosen on the entry itself. */
enum class EntryUpdateMode {
    FOLLOW_RULES,

    /** Checked by every library update that covers the entry, whatever its categories and skip rules say. */
    ALWAYS,

    /** Never checked by a library update; only a refresh from the entry's own screen checks it. */
    NEVER,
}
