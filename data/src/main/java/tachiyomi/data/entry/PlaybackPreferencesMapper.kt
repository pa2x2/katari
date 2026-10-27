package tachiyomi.data.entry

import tachiyomi.domain.entry.model.PictureFraming
import tachiyomi.domain.entry.model.PlaybackPreferences
import tachiyomi.domain.entry.model.PlayerEngineOverride
import tachiyomi.domain.entry.model.PlayerQualityMode
import tachiyomi.domain.entry.model.SubtitleTextEdge

object PlaybackPreferencesMapper {
    fun mapPreferences(
        @Suppress("UNUSED_PARAMETER")
        id: Long,
        entryId: Long,
        dubKey: String?,
        streamKey: String?,
        sourceQualityKey: String?,
        subtitleKey: String?,
        playerQualityMode: String,
        playerQualityHeight: Long?,
        subtitleOffsetX: Double?,
        subtitleOffsetY: Double?,
        subtitleTextSize: Double?,
        subtitleTextColor: Long?,
        subtitleBackgroundColor: Long?,
        subtitleBackgroundOpacity: Double?,
        updatedAt: Long,
        playerEngine: String?,
        mpvHintShown: Boolean,
        serverKey: String?,
        pictureFraming: String?,
        pictureZoom: Double?,
        picturePanX: Double?,
        picturePanY: Double?,
        subtitleTextEdge: String?,
    ): PlaybackPreferences {
        return PlaybackPreferences(
            entryId = entryId,
            dubKey = dubKey,
            serverKey = serverKey,
            streamKey = streamKey,
            sourceQualityKey = sourceQualityKey,
            subtitleKey = subtitleKey,
            playerQualityMode = playerQualityMode.fromDatabaseValue(),
            playerQualityHeight = playerQualityHeight?.toInt(),
            subtitleOffsetX = subtitleOffsetX,
            subtitleOffsetY = subtitleOffsetY,
            subtitleTextSize = subtitleTextSize,
            subtitleTextColor = subtitleTextColor?.toInt(),
            subtitleBackgroundColor = subtitleBackgroundColor?.toInt(),
            subtitleBackgroundOpacity = subtitleBackgroundOpacity,
            subtitleTextEdge = subtitleTextEdge?.toSubtitleTextEdge(),
            playerEngine = playerEngine?.toPlayerEngine(),
            mpvHintShown = mpvHintShown,
            pictureFraming = decodePictureFraming(pictureFraming, pictureZoom, picturePanX, picturePanY),
            updatedAt = updatedAt,
        )
    }

    /** Fit stores no name; a zoom keeps its scale and pan beside its name. */
    fun encodePictureFraming(framing: PictureFraming): EncodedPictureFraming {
        return when (framing) {
            PictureFraming.Fit -> EncodedPictureFraming(name = null)
            PictureFraming.Fill -> EncodedPictureFraming(name = "fill")
            is PictureFraming.Zoom -> EncodedPictureFraming(
                name = "zoom",
                zoom = framing.scale,
                panX = framing.panX,
                panY = framing.panY,
            )
        }
    }

    // A value this version doesn't know, or a zoom missing its scale, fits the picture.
    private fun decodePictureFraming(name: String?, zoom: Double?, panX: Double?, panY: Double?): PictureFraming {
        return when (name) {
            "fill" -> PictureFraming.Fill
            "zoom" -> zoom?.let { PictureFraming.Zoom(it, panX ?: 0.0, panY ?: 0.0) } ?: PictureFraming.Fit
            else -> PictureFraming.Fit
        }
    }

    fun encodePlayerQualityMode(mode: PlayerQualityMode): String {
        return when (mode) {
            PlayerQualityMode.AUTO -> "auto"
            PlayerQualityMode.SPECIFIC_HEIGHT -> "specific_height"
        }
    }

    fun encodePlayerEngine(engine: PlayerEngineOverride?): String? {
        return when (engine) {
            PlayerEngineOverride.BUILT_IN -> "built_in"
            PlayerEngineOverride.MPV -> "mpv"
            null -> null
        }
    }

    fun encodeSubtitleTextEdge(edge: SubtitleTextEdge?): String? {
        return when (edge) {
            SubtitleTextEdge.NONE -> "none"
            SubtitleTextEdge.OUTLINE -> "outline"
            SubtitleTextEdge.SHADOW -> "shadow"
            null -> null
        }
    }

    // A value this version doesn't know keeps the outline.
    private fun String.toSubtitleTextEdge(): SubtitleTextEdge? {
        return when (this) {
            "none" -> SubtitleTextEdge.NONE
            "outline" -> SubtitleTextEdge.OUTLINE
            "shadow" -> SubtitleTextEdge.SHADOW
            else -> null
        }
    }

    // A value this version doesn't know leaves the choice to the player.
    private fun String.toPlayerEngine(): PlayerEngineOverride? {
        return when (this) {
            "built_in" -> PlayerEngineOverride.BUILT_IN
            "mpv" -> PlayerEngineOverride.MPV
            else -> null
        }
    }

    private fun String.fromDatabaseValue(): PlayerQualityMode {
        return when (this) {
            "specific_height" -> PlayerQualityMode.SPECIFIC_HEIGHT
            else -> PlayerQualityMode.AUTO
        }
    }
}

data class EncodedPictureFraming(
    val name: String?,
    val zoom: Double? = null,
    val panX: Double? = null,
    val panY: Double? = null,
)
