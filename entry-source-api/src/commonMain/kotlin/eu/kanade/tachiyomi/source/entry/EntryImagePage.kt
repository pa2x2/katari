package eu.kanade.tachiyomi.source.entry

/**
 * Image page metadata for Entry-era manga-like media.
 *
 * @property index unique zero-based position in reading order.
 * @property url stable page or intermediate resolution URL.
 * @property imageUrl final image URL when it is already known.
 */
data class EntryImagePage(
    val index: Int,
    val url: String = "",
    val imageUrl: String? = null,
)
