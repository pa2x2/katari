package eu.kanade.tachiyomi.data.track

data class EntryTrackingSource(
    val sourceId: Long,
    val name: String,
    val lang: String,
    val legacyClassName: String?,
)
