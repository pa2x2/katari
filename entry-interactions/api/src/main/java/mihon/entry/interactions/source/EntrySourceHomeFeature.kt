package mihon.entry.interactions

interface EntrySourceHomeFeature {
    fun resolve(sourceId: Long): EntrySourceHomeResolution
}

sealed interface EntrySourceHomeResolution {
    val sourceId: Long

    data class Available(
        override val sourceId: Long,
        val sourceName: String,
        val url: String,
    ) : EntrySourceHomeResolution

    data class Missing(override val sourceId: Long) : EntrySourceHomeResolution
    data class Unsupported(override val sourceId: Long) : EntrySourceHomeResolution
    data class NoUrl(override val sourceId: Long) : EntrySourceHomeResolution
    data class Failed(override val sourceId: Long, val cause: Throwable) : EntrySourceHomeResolution
}
