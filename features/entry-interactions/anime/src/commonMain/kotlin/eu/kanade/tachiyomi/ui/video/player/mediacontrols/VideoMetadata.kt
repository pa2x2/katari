package eu.kanade.tachiyomi.ui.video.player.mediacontrols

import tachiyomi.domain.entry.model.EntryCover

/**
 * The episode as media controls show it: [title] is the episode's, [series] the title it belongs to.
 * [episodePreviewUrl] is the episode's own still where its source has one, served by the source like [cover].
 */
internal data class VideoMetadata(
    val title: String?,
    val series: String?,
    val episodePreviewUrl: String?,
    val cover: EntryCover,
) {
    val artworkUrl: String?
        get() = cover.url
}
