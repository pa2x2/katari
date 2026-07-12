package tachiyomi.data.entry

import tachiyomi.domain.entry.model.DownloadPreferences
import tachiyomi.domain.entry.model.VideoDownloadQualityMode

object DownloadPreferencesMapper {
    fun mapPreferences(
        @Suppress("UNUSED_PARAMETER")
        id: Long,
        entryId: Long,
        dubKey: String?,
        streamKey: String?,
        subtitleKey: String?,
        qualityMode: String,
        updatedAt: Long,
    ): DownloadPreferences {
        return DownloadPreferences(
            entryId = entryId,
            dubKey = dubKey,
            streamKey = streamKey,
            subtitleKey = subtitleKey,
            qualityMode = qualityMode.fromDatabaseValue(),
            updatedAt = updatedAt,
        )
    }

    fun encodeQualityMode(mode: VideoDownloadQualityMode): String {
        return when (mode) {
            VideoDownloadQualityMode.BEST -> "best"
            VideoDownloadQualityMode.BALANCED -> "balanced"
            VideoDownloadQualityMode.DATA_SAVING -> "data_saving"
        }
    }

    private fun String.fromDatabaseValue(): VideoDownloadQualityMode {
        return when (this) {
            "best" -> VideoDownloadQualityMode.BEST
            "data_saving" -> VideoDownloadQualityMode.DATA_SAVING
            else -> VideoDownloadQualityMode.BALANCED
        }
    }
}
