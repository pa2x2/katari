package tachiyomi.domain.entry.model

data class PlaybackPreferences(
    val entryId: Long,
    val dubKey: String?,
    val serverKey: String? = null,
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
    /** Null while the player chooses by itself. */
    val playerEngine: PlayerEngineOverride? = null,
    /** The hint that the mpv add-on would play this title better has been shown; it shows once per title. */
    val mpvHintShown: Boolean = false,
    val pictureFraming: PictureFraming = PictureFraming.Fit,
    val updatedAt: Long,
)
