package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.Player
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerOverlay
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSettingsSection
import eu.kanade.tachiyomi.ui.video.player.components.videoPlayerEpisodeLine
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerSeekDirection
import eu.kanade.tachiyomi.ui.video.player.markers.VideoPlayerChapter
import eu.kanade.tachiyomi.ui.video.player.markers.VideoPlayerSkipOffer
import eu.kanade.tachiyomi.ui.video.player.markers.chapterAt
import eu.kanade.tachiyomi.ui.video.player.picture.toggled
import eu.kanade.tachiyomi.ui.video.player.playback.coerceToPlaybackDuration
import eu.kanade.tachiyomi.ui.video.player.rotatedVideoPlayerOrientation
import eu.kanade.tachiyomi.ui.video.player.videoPlaybackSpeedLabel
import tachiyomi.domain.entry.model.EntryChapter

/**
 * The player controls over the video: top bar, transport, timeline with scrubbing, seek preview, the source's markers
 * and the chapter playing, the skip button, the episode drawer and the lock. Hidden in picture-in-picture, in the
 * subtitle editor and until the first frame.
 */
@Composable
internal fun VideoPlayerControlsLayer(
    current: VideoPlayerViewModel.State.Ready,
    ui: VideoPlayerWatchUiState,
    seekPreview: VideoPlayerSeekPreviewSession,
    currentPlayer: Player,
    host: VideoPlayerScreenHost,
    viewModel: VideoPlayerViewModel,
    actions: VideoPlayerWatchActions,
    isInPictureInPictureMode: Boolean,
    endScreenVisible: Boolean,
    displayedPositionMs: Long,
    hidePlayerChrome: Boolean,
    seekStepSeconds: Int,
    chapters: List<VideoPlayerChapter>,
    skipOffer: VideoPlayerSkipOffer?,
    onSkip: () -> Unit,
) {
    val context = LocalContext.current
    // The chapters cover every marker; without them the markers alone cut the timeline.
    val markerBoundariesMs = remember(chapters, current.stream.markers) {
        if (chapters.isNotEmpty()) {
            chapters.drop(1).map { it.startMs }
        } else {
            current.stream.markers.flatMap { listOf(it.startMs, it.endMs) }.distinct()
        }
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
            onScrubStarted = {
                ui.registerControllerInteraction(true)
                ui.isScrubbing = true
                ui.scrubPositionMs = ui.playbackSnapshot.positionMs
                seekPreview.onScrubStarted(
                    positionMs = ui.playbackSnapshot.positionMs,
                    durationMs = ui.playbackSnapshot.durationMs,
                )
            },
            onScrubPositionChange = { positionMs ->
                val targetPositionMs = positionMs.coerceToPlaybackDuration(ui.playbackSnapshot.durationMs)
                ui.scrubPositionMs = targetPositionMs
                seekPreview.onScrubPositionChange(targetPositionMs, ui.playbackSnapshot.durationMs)
            },
            onScrubFinished = {
                val targetPositionMs = ui.scrubPositionMs.coerceToPlaybackDuration(
                    ui.playbackSnapshot.durationMs,
                )
                ui.isScrubbing = false
                seekPreview.onScrubFinished()
                ui.scrubPositionMs = targetPositionMs
                ui.playbackSnapshot = ui.playbackSnapshot.copy(positionMs = targetPositionMs)
                host.pictureInPicture.latestSnapshot = ui.playbackSnapshot
                currentPlayer.seekTo(targetPositionMs)
                ui.registerControllerInteraction(true)
            },
            showsRemainingTime = host.visit.showsRemainingTime,
            onToggleRemainingTime = {
                host.visit.showsRemainingTime = !host.visit.showsRemainingTime
                host.preferences.showsRemainingTime.set(host.visit.showsRemainingTime)
                ui.registerControllerInteraction(true)
            },
            markerBoundariesMs = markerBoundariesMs,
            chapter = chapters.chapterAt(displayedPositionMs),
            onOpenChapters = { actions.openSettings(VideoPlayerSettingsSection.Chapters) },
            skipOffer = skipOffer,
            onSkip = onSkip,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
