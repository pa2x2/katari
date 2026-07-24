package mihon.entry.interactions.anime.download.model

import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.VideoStreamType
import kotlinx.serialization.Serializable

@Serializable
internal data class AnimeDownloadManifest(
    val animeId: Long,
    val episodeId: Long,
    val animeTitle: String,
    val episodeTitle: String,
    val originalEpisodeUrl: String,
    val qualityMode: String,
    val selection: PlaybackSelection,
    val video: DownloadedVideo,
    val subtitles: List<DownloadedSubtitle>,
    val artifacts: List<DownloadedArtifact> = emptyList(),
)

@Serializable
internal data class DownloadedArtifact(
    val fileName: String,
    val storedSize: Long,
)

@Serializable
internal data class DownloadedVideo(
    val fileName: String,
    val sourceUrl: String,
    val headers: Map<String, String>,
    val label: String,
    val streamType: VideoStreamType,
    val mimeType: String?,
)

@Serializable
internal data class DownloadedSubtitle(
    val key: String,
    val label: String,
    val language: String?,
    val mimeType: String?,
    val fileName: String,
    val isDefault: Boolean,
    val isForced: Boolean,
)
