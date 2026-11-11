package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerBarReach
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerSeekDirection
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerSeekFeedbackState
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerSideGestureFeedbackState
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerSideGestureType
import eu.kanade.tachiyomi.ui.video.player.gesture.nextVideoPlayerSeekFeedbackState
import eu.kanade.tachiyomi.ui.video.player.playback.VideoPlayerPlaybackSnapshot
import eu.kanade.tachiyomi.ui.video.player.subtitle.VideoSubtitleAppearance
import tachiyomi.domain.entry.model.PictureFraming

/**
 * The watch screen's state for one playback: what is open over the video, scrubbing, the latest playback snapshot and
 * gesture feedback. The screen makes a new one whenever the episode, stream or subtitle set changes, which starts
 * every value over.
 */
@Stable
internal class VideoPlayerWatchUiState(
    subtitleAppearance: VideoSubtitleAppearance,
    resumePositionMs: Long,
) {
    var controlsVisible by mutableStateOf(true)
    var startupOverlayVisible by mutableStateOf(true)

    /** A frame reached the screen. The startup overlay also goes when the player fails before one does. */
    var frameShown by mutableStateOf(false)
    var settingsVisible by mutableStateOf(false)
    var episodesDrawerVisible by mutableStateOf(false)
    var subtitleEditorVisible by mutableStateOf(false)
    var subtitleEditorDraft by mutableStateOf(subtitleAppearance)
    var subtitleEditorKeepsInPicture by mutableStateOf(false)
    var resumePlaybackAfterSubtitleEditor by mutableStateOf(false)
    var isScrubbing by mutableStateOf(false)
    var scrubPositionMs by mutableStateOf(resumePositionMs.coerceAtLeast(0L))
    var playbackSnapshot by mutableStateOf(
        VideoPlayerPlaybackSnapshot(
            positionMs = resumePositionMs.coerceAtLeast(0L),
        ),
    )

    /** Bumped by every interaction with the controls, which restarts their auto-hide delay. */
    var controllerInteractionSequence by mutableStateOf(0L)

    /**
     * The [controllerInteractionSequence] a paused start showed the controls at. They stay up until the next interaction
     * moves the sequence on, as the showcase captures them over the paused frame for longer than the auto-hide delay.
     * Null without a paused start.
     */
    var controlsHeldAtInteraction by mutableStateOf<Long?>(null)

    var seekFeedbackState by mutableStateOf<VideoPlayerSeekFeedbackState?>(null)
    var ignoreNextGestureSeekTapUp by mutableStateOf(false)
    var sideGestureFeedbackState by mutableStateOf<VideoPlayerSideGestureFeedbackState?>(null)

    /** A mouse rests on one of the controls, which keeps them from hiding after a while. */
    var pointerOverControls by mutableStateOf(false)

    /** How far the bars reach in from the player's edges, for a mouse; null until they have been laid out. */
    var barReach by mutableStateOf<VideoPlayerBarReach?>(null)

    /** Where a mouse is over the timeline; null while it isn't. */
    var timelinePointerMs by mutableStateOf<Long?>(null)

    /** Where the timeline previews a hovering mouse, once it has rested on the bar; null while it doesn't. */
    var timelineHoverMs by mutableStateOf<Long?>(null)

    /** What a pinch shows until its fingers lift and the framing it saved reaches the screen. */
    var picturePreview by mutableStateOf<PictureFraming?>(null)

    private var seekFeedbackSequence by mutableStateOf(0L)
    private var sideGestureFeedbackSequence by mutableStateOf(0L)

    fun registerControllerInteraction(shouldShowControls: Boolean) {
        if (shouldShowControls) {
            controlsVisible = true
        }
        controllerInteractionSequence += 1L
    }

    fun triggerSeekFeedback(direction: VideoPlayerSeekDirection, movedMs: Long, hidePlayerChrome: Boolean) {
        seekFeedbackSequence += 1L
        seekFeedbackState = nextVideoPlayerSeekFeedbackState(
            previousState = seekFeedbackState,
            direction = direction,
            movedMs = movedMs,
            hidePlayerChrome = hidePlayerChrome,
            sequence = seekFeedbackSequence,
            updatedAtMillis = System.currentTimeMillis(),
        )
    }

    fun triggerSideGestureFeedback(
        type: VideoPlayerSideGestureType,
        level: Int,
        maxLevel: Int,
        boostFromLevel: Int? = null,
        boostPercent: Int? = null,
    ) {
        sideGestureFeedbackSequence += 1L
        sideGestureFeedbackState = VideoPlayerSideGestureFeedbackState(
            type = type,
            level = level,
            maxLevel = maxLevel,
            boostFromLevel = boostFromLevel,
            boostPercent = boostPercent,
            sequence = sideGestureFeedbackSequence,
        )
    }
}
