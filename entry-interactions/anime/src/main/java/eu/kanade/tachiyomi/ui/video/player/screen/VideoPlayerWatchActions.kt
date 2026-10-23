package eu.kanade.tachiyomi.ui.video.player.screen

import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSettingsSection
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerSeekDirection

/** What the watch screen's layers ask of the screen beyond the state they change themselves. */
internal class VideoPlayerWatchActions(
    val previousEpisode: () -> Unit,
    val nextEpisode: () -> Unit,

    /** Opens the settings at a section, or at their start for null. */
    val openSettings: (VideoPlayerSettingsSection?) -> Unit,
    val dismissSettings: () -> Unit,
    val togglePlayback: () -> Unit,

    /** Seeks one step from the seek buttons, with their feedback. */
    val seekFromControls: (VideoPlayerSeekDirection) -> Unit,
    val endSpeedBoost: () -> Unit,
)
