package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerOverlay
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSettingsSection
import eu.kanade.tachiyomi.ui.video.player.components.videoPlayerEpisodeLine
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerSeekDirection
import eu.kanade.tachiyomi.ui.video.player.markers.VideoPlayerSkipCountdown
import eu.kanade.tachiyomi.ui.video.player.markers.VideoPlayerSkipOffer
import eu.kanade.tachiyomi.ui.video.player.picture.toggled
import eu.kanade.tachiyomi.ui.video.player.rotatedVideoPlayerOrientation
import eu.kanade.tachiyomi.ui.video.player.videoPlaybackSpeedLabel
import tachiyomi.domain.entry.model.EntryChapter

/**
 * The player controls over the video: top bar, transport, timeline with scrubbing, seek preview and the source's
 * markers, the skip button, the episode drawer and the lock. Hidden in picture-in-picture, in the subtitle editor and
 * until the first frame.
 */
@Composable
internal fun VideoPlayerControlsLayer(
    current: VideoPlayerViewModel.State.Ready,
    ui: VideoPlayerWatchUiState,
    seekPreview: VideoPlayerSeekPreviewSession,
    scrub: VideoPlayerScrub,
    host: VideoPlayerScreenHost,
    viewModel: VideoPlayerViewModel,
    actions: VideoPlayerWatchActions,
    isInPictureInPictureMode: Boolean,
    endScreenVisible: Boolean,
    displayedPositionMs: Long,
    hidePlayerChrome: Boolean,
    seekStepSeconds: Int,
    skipOffer: VideoPlayerSkipOffer?,
    skipCountdown: VideoPlayerSkipCountdown?,
    onSkip: () -> Unit,
) {
    val context = LocalContext.current
    val markerBoundariesMs = remember(current.stream.markers) {
        current.stream.markers.flatMap { listOf(it.startMs, it.endMs) }.distinct()
    }
    if (!isInPictureInPictureMode && !ui.subtitleEditorVisible && !ui.startupOverlayVisible) {
        VideoPlayerOverlay(
            visible = ui.controlsVisible,
            endScreenVisible = endScreenVisible,
            locked = host.visit.controlsLocked,
            videoTitle = current.entryTitle,
            episodeName = videoPlayerEpisodeLine(current.memberTitle, current.chapterName),
            playbackSnapshot = ui.playbackSnapshot,
            displayedPositionMs = displayedPositionMs,
            isScrubbing = ui.isScrubbing,
            episodesDrawerVisible = ui.episodesDrawerVisible,
            currentEpisodeId = current.chapterId,
            episodeListItems = current.chapterListItems,
            expandedMemberIds = viewModel.expandedMemberIds.collectAsState().value,
            expandedGroupIds = viewModel.expandedGroupIds.collectAsState().value,
            playbackStateByEpisodeId = current.playbackStateByChapterId,
            sourceAvailable = current.sourceAvailable,
            hasPreviousEpisode = current.previousChapterId != null,
            hasNextEpisode = current.nextChapterId != null,
            seekStepSeconds = seekStepSeconds,
            playbackSpeedLabel = videoPlaybackSpeedLabel(current.playback.sessionPlaybackSpeed),
            picture = current.playback.pictureFraming,
            showSubtitles = current.playback.hasSubtitleSelection,
            seekFeedbackState = ui.seekFeedbackState,
            seekPreviewState = seekPreview.state,
            seekPreviewPlayer = seekPreview.player,
            seekPreviewAspectRatio = seekPreview.aspectRatio,
            sideGestureFeedbackState = ui.sideGestureFeedbackState,
            hideChromeForSeekFeedback = hidePlayerChrome,
            onSeekFeedbackDismissed = {
                ui.ignoreNextGestureSeekTapUp = false
                ui.seekFeedbackState = null
            },
            onSideGestureFeedbackDismissed = {
                ui.sideGestureFeedbackState = null
            },
            onBack = host.onClose,
            showPictureInPictureButton = host.pictureInPicture.showsButton,
            onEnterPictureInPicture = host.pictureInPicture::enterFromControls,
            onToggleLock = {
                actions.endSpeedBoost()
                host.visit.controlsLocked = !host.visit.controlsLocked
                ui.controlsVisible = true
                ui.settingsVisible = false
                ui.episodesDrawerVisible = false
                ui.isScrubbing = false
                ui.registerControllerInteraction(true)
            },
            onOpenSettings = { actions.openSettings(null) },
            onOpenSpeed = { actions.openSettings(VideoPlayerSettingsSection.Speed) },
            onOpenSubtitles = { actions.openSettings(VideoPlayerSettingsSection.Subtitles) },
            onRotate = {
                host.visit.orientationOverride =
                    rotatedVideoPlayerOrientation(context.resources.configuration.orientation)
                ui.registerControllerInteraction(true)
            },
            onTogglePicture = {
                viewModel.setPictureFraming(current.playback.pictureFraming.toggled())
                ui.registerControllerInteraction(true)
            },
            childWebView = current.childWebView,
            onChildWebViewAction = { action, resolution ->
                host.onOpenWebView(
                    action,
                    resolution,
                    current.entryTitle,
                )
            },
            onOpenEpisodes = {
                actions.endSpeedBoost()
                ui.settingsVisible = false
                ui.episodesDrawerVisible = true
                ui.registerControllerInteraction(true)
            },
            onDismissEpisodes = {
                ui.episodesDrawerVisible = false
                ui.registerControllerInteraction(true)
            },
            onMemberToggled = viewModel::toggleMemberExpanded,
            onGroupToggled = viewModel::toggleGroupExpanded,
            onEpisodeSelected = { episode: EntryChapter ->
                if (episode.id != current.chapterId) {
                    actions.endSpeedBoost()
                    ui.episodesDrawerVisible = false
                    ui.controlsVisible = !host.visit.controlsLocked
                    actions.leaveEpisodeEnd()
                    host.playback.flush()
                    viewModel.playEpisode(
                        visibleEntryId = current.visibleEntryId,
                        ownerEntryId = episode.entryId,
                        episodeId = episode.id,
                    )
                }
            },
            onPreviousEpisode = actions.previousEpisode,
            onSeekBackward = { actions.seekFromControls(VideoPlayerSeekDirection.Backward) },
            onTogglePlayback = actions.togglePlayback,
            onSeekForward = { actions.seekFromControls(VideoPlayerSeekDirection.Forward) },
            onNextEpisode = actions.nextEpisode,
            onScrubStarted = scrub::start,
            onScrubPositionChange = scrub::moveTo,
            onScrubFinished = scrub::finish,
            swipeSeekDeltaMs = scrub.swipeDeltaMs,
            showsRemainingTime = host.visit.showsRemainingTime,
            onToggleRemainingTime = {
                host.visit.showsRemainingTime = !host.visit.showsRemainingTime
                host.preferences.showsRemainingTime.set(host.visit.showsRemainingTime)
                ui.registerControllerInteraction(true)
            },
            markerBoundariesMs = markerBoundariesMs,
            skipOffer = skipOffer,
            skipCountdown = skipCountdown,
            onSkip = onSkip,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
