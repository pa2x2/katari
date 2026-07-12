package eu.kanade.tachiyomi.source.entry

/**
 * Classification of a URI resolved by a [ResolvableSource].
 */
sealed interface EntryUriType {
    data object Entry : EntryUriType
    data object Chapter : EntryUriType
    data object Unknown : EntryUriType
}

/**
 * Optional capability for sources that may open an entry or chapter for a given URI.
 */
interface ResolvableSource : UnifiedSource {

    /**
     * Returns what the given URI may open.
     * Returns [EntryUriType.Unknown] if the source cannot resolve the URI.
     */
    fun getUriType(uri: String): EntryUriType

    /**
     * Called if [getUriType] is [EntryUriType.Entry].
     * Returns the corresponding entry, if possible.
     */
    suspend fun getEntry(uri: String): SEntry?

    /**
     * Called if [getUriType] is [EntryUriType.Chapter].
     * Returns the corresponding chapter, if possible.
     */
    suspend fun getChapter(uri: String): SEntryChapter?
}
