package eu.kanade.tachiyomi.source.entry

/**
 * Optional capability for sources that expose static preview images for an entry.
 *
 * Preview images describe the title as a whole and are independent from chapter media.
 */
interface EntryPreviewSource : UnifiedSource {

    /**
     * Returns static preview images for [entry] in display order.
     */
    suspend fun getEntryPreview(entry: SEntry): List<EntryPreviewImage>
}

/**
 * A static image supplied by an [EntryPreviewSource].
 *
 * [title] and [url] are optional source metadata for clients that present richer preview surfaces.
 */
data class EntryPreviewImage(
    val index: Int,
    val imageUrl: String,
    val title: String? = null,
    val url: String? = null,
)
