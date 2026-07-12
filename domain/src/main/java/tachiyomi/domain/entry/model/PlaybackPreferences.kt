package tachiyomi.domain.entry.model

data class PlaybackPreferences(
    val entryId: Long,
    val dubKey: String?,
    val streamKey: String?,
    val sourceQualityKey: String?,
    val subtitleKey: String?,
    val playerQualityMode: PlayerQualityMode,
    val playerQualityHeight: Int?,
    val subtitleOffsetX: Double? = null,
    val subtitleOffsetY: Double? = null,
    val subtitleTextSize: Double? = null,
    val subtitleTextColor: Int? = null,
    val subtitleBackgroundColor: Int? = null,
    val subtitleBackgroundOpacity: Double? = null,
    val updatedAt: Long,
)
