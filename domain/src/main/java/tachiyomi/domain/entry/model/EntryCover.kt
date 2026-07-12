package tachiyomi.domain.entry.model

/**
 * Contains the required data for EntryCoverFetcher
 */
data class EntryCover(
    val entryId: Long,
    val sourceId: Long,
    val isFavorite: Boolean,
    val url: String?,
    val lastModified: Long,
)

fun Entry.asEntryCover(): EntryCover {
    return EntryCover(
        entryId = id,
        sourceId = source,
        isFavorite = favorite,
        url = thumbnailUrl,
        lastModified = coverLastModified,
    )
}
