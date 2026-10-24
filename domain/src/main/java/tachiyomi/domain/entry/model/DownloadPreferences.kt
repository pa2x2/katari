package tachiyomi.domain.entry.model

import tachiyomi.domain.entry.model.VideoDownloadQualityMode

data class DownloadPreferences(
    val entryId: Long,
    val dubKey: String?,
    val serverKey: String? = null,
    val streamKey: String?,
    val subtitleKey: String?,
    val audioKey: String? = null,
    val qualityMode: VideoDownloadQualityMode,
    val updatedAt: Long,
)
