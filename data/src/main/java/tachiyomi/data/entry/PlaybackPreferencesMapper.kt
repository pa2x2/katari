package tachiyomi.data.entry

import tachiyomi.domain.entry.model.PlaybackPreferences
import tachiyomi.domain.entry.model.PlayerEngineOverride
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
        playerEngine: String?,
        mpvHintShown: Boolean,
        serverKey: String?,
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
            playerEngine = playerEngine?.toPlayerEngine(),
            mpvHintShown = mpvHintShown,
            updatedAt = updatedAt,
        )
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
