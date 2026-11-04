package eu.kanade.tachiyomi.ui.video.player.mediacontrols

/** The episode as media controls show it: [title] is the episode's, [series] the title it belongs to. */
internal data class VideoMetadata(
    val title: String?,
    val series: String?,
    val artworkUrl: String?,
)
