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
    /** Preferred thumbnail orientation for this source. */
    val itemOrientation: EntryItemOrientation
        get() = EntryItemOrientation.VERTICAL
}

/** Returns the source-provided orientation or [EntryItemOrientation.VERTICAL] by default. */
fun UnifiedSource.entryItemOrientation(): EntryItemOrientation {
    return (this as? EntryItemOrientationProvider)?.itemOrientation ?: EntryItemOrientation.VERTICAL
}
