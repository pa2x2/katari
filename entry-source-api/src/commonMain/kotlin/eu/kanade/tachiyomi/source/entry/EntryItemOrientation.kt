package eu.kanade.tachiyomi.source.entry

/**
 * Preferred orientation for source browse, library, and feed thumbnails.
 */
enum class EntryItemOrientation {
    VERTICAL,
    HORIZONTAL,
}

/**
 * Optional capability for sources that provide non-vertical browse thumbnails.
 */
interface EntryItemOrientationProvider {
    val itemOrientation: EntryItemOrientation
        get() = EntryItemOrientation.VERTICAL
}

fun UnifiedSource.entryItemOrientation(): EntryItemOrientation {
    return (this as? EntryItemOrientationProvider)?.itemOrientation ?: EntryItemOrientation.VERTICAL
}
