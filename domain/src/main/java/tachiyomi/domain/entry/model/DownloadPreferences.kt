package tachiyomi.domain.entry.model

import tachiyomi.domain.entry.model.VideoDownloadQualityMode

data class DownloadPreferences(
    val entryId: Long,
    val dubKey: String?,
    val streamKey: String?,
    val subtitleKey: String?,
    val qualityMode: VideoDownloadQualityMode,
    val updatedAt: Long,
)
