package tachiyomi.data.entry

import tachiyomi.domain.entry.model.PlaybackPreferences
import tachiyomi.domain.entry.model.PlayerQualityMode

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
    ): PlaybackPreferences {
        return PlaybackPreferences(
            entryId = entryId,
            dubKey = dubKey,
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
            updatedAt = updatedAt,
        )
    }

    fun encodePlayerQualityMode(mode: PlayerQualityMode): String {
        return when (mode) {
            PlayerQualityMode.AUTO -> "auto"
            PlayerQualityMode.SPECIFIC_HEIGHT -> "specific_height"
        }
    }

    private fun String.fromDatabaseValue(): PlayerQualityMode {
        return when (this) {
            "specific_height" -> PlayerQualityMode.SPECIFIC_HEIGHT
            else -> PlayerQualityMode.AUTO
        }
    }
}
