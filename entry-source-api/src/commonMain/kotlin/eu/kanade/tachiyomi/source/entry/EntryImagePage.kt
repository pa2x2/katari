package eu.kanade.tachiyomi.source.entry

/**
 * Image page metadata for Entry-era manga-like media.
 */
data class EntryImagePage(
    val index: Int,
    val url: String = "",
    val imageUrl: String? = null,
)
