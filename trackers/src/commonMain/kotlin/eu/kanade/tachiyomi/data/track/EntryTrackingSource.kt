package eu.kanade.tachiyomi.data.track

/** @param className the class the extension wrote the source as; null for a class with no name. */
data class EntryTrackingSource(
    val sourceId: Long,
    val name: String,
    val lang: String,
    val className: String?,
)
