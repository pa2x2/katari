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

    /** Seeks one step from a double tap, hiding the controls behind the seek feedback. */
    val gestureSeek: (VideoPlayerSeekDirection) -> Unit,

    /** Seeks one step from a key, with the seek feedback over the controls as they are. */
    val seekStep: (VideoPlayerSeekDirection) -> Unit,
    val toggleFullScreen: () -> Unit,
    val openEpisodes: () -> Unit,

    /** Presses Skip, which does nothing while nothing is offered. */
    val skip: () -> Unit,
    val endSpeedBoost: () -> Unit,

    /** Called before another episode is asked for, so one that fails to open isn't retried from the end screen. */
    val leaveEpisodeEnd: () -> Unit,
)
