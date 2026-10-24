package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import androidx.media3.common.Player
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerEpisodeListEntry
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerSeekFeedbackState
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerSideGestureFeedbackState
import eu.kanade.tachiyomi.ui.video.player.markers.VideoPlayerSkipOffer
import eu.kanade.tachiyomi.ui.video.player.playback.VideoPlayerPlaybackSnapshot
import mihon.entry.interactions.source.EntryChildWebViewAction
import mihon.entry.interactions.source.EntryChildWebViewResolution
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState

private val overlayBarsSlideAnimationSpec = tween<IntOffset>(190)
private val overlayBarsFadeAnimationSpec = tween<Float>(125)
private val overlayCenterFadeAnimationSpec = tween<Float>(110)

@Composable
internal fun VideoPlayerOverlay(
    visible: Boolean,
    endScreenVisible: Boolean,
    locked: Boolean,
    videoTitle: String,
    episodeName: String,
    playbackSnapshot: VideoPlayerPlaybackSnapshot,
    displayedPositionMs: Long,
    isScrubbing: Boolean,
    episodesDrawerVisible: Boolean,
    currentEpisodeId: Long,
    episodeListItems: List<VideoPlayerEpisodeListEntry>,
    expandedMemberIds: Set<Long>,
    playbackStateByEpisodeId: Map<Long, EntryProgressState>,
    sourceAvailable: Boolean,
    hasPreviousEpisode: Boolean,
    hasNextEpisode: Boolean,
    seekStepSeconds: Int,
    playbackSpeedLabel: String,
    showSubtitles: Boolean,
    seekFeedbackState: VideoPlayerSeekFeedbackState?,
    seekPreviewState: VideoPlayerSeekPreviewState?,
    seekPreviewPlayer: Player?,
    seekPreviewAspectRatio: Float?,
    sideGestureFeedbackState: VideoPlayerSideGestureFeedbackState?,
    hideChromeForSeekFeedback: Boolean,
    onSeekFeedbackDismissed: () -> Unit,
    onSideGestureFeedbackDismissed: () -> Unit,
    onBack: () -> Unit,
    showPictureInPictureButton: Boolean,
    onEnterPictureInPicture: () -> Unit,
    onToggleLock: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onRotate: () -> Unit,
    childWebView: EntryChildWebViewResolution.Available?,
    onChildWebViewAction: (EntryChildWebViewAction, EntryChildWebViewResolution.Available) -> Unit,
    onOpenEpisodes: () -> Unit,
    onDismissEpisodes: () -> Unit,
    onEpisodeSelected: (EntryChapter) -> Unit,
    onMemberToggled: (Long) -> Unit,
    onPreviousEpisode: () -> Unit,
    onSeekBackward: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSeekForward: () -> Unit,
    onNextEpisode: () -> Unit,
    onScrubStarted: () -> Unit,
    onScrubPositionChange: (Long) -> Unit,
    onScrubFinished: () -> Unit,
    showsRemainingTime: Boolean,
    onToggleRemainingTime: () -> Unit,
    markerBoundariesMs: List<Long>,
    skipOffer: VideoPlayerSkipOffer?,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chromeVisible = visible && !hideChromeForSeekFeedback && !locked
    // The end screen keeps the top bar for leaving or opening settings; its own Play replaces the transport controls.
    val topBarVisible = chromeVisible || (endScreenVisible && !locked)
    val transportVisible = chromeVisible && !endScreenVisible
    val lockedChromeVisible = visible && !hideChromeForSeekFeedback && locked
    // A marker's first seconds show the button by itself; after that it comes and goes with the controls.
    val skipVisible = skipOffer != null &&
        !locked &&
        !endScreenVisible &&
        !hideChromeForSeekFeedback &&
        !isScrubbing &&
        (skipOffer.prominent || chromeVisible)

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f),
        ) {
            AnimatedVisibility(
                visible = topBarVisible,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = overlayBarsSlideAnimationSpec,
                ) + fadeIn(animationSpec = overlayBarsFadeAnimationSpec),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = overlayBarsSlideAnimationSpec,
                ) + fadeOut(animationSpec = overlayBarsFadeAnimationSpec),
            ) {
                VideoPlayerChromeContainer(
                    contentAlignment = Alignment.TopCenter,
                ) {
                    VideoPlayerTopBar(
                        videoTitle = videoTitle,
                        episodeName = episodeName,
                        onBack = onBack,
                        showPictureInPictureButton = showPictureInPictureButton,
                        onEnterPictureInPicture = onEnterPictureInPicture,
                        onToggleLock = onToggleLock,
                        onOpenSettings = onOpenSettings,
                        childWebView = childWebView,
                        onChildWebViewAction = onChildWebViewAction,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            AnimatedVisibility(
                visible = skipVisible,
                modifier = Modifier.align(Alignment.End),
                enter = fadeIn(animationSpec = overlayBarsFadeAnimationSpec),
                exit = fadeOut(animationSpec = overlayBarsFadeAnimationSpec),
            ) {
                skipOffer?.let { offer ->
                    VideoPlayerSkipButton(
                        offer = offer,
                        onClick = onSkip,
                        modifier = Modifier
                            .videoPlayerSafeContentPadding(includeBottom = !transportVisible)
                            .padding(
                                end = KatariTheme.spacing.l,
                                bottom = if (transportVisible) KatariTheme.spacing.s else KatariTheme.spacing.l,
                            ),
                    )
                }
            }

            AnimatedVisibility(
                visible = transportVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = overlayBarsSlideAnimationSpec,
                ) + fadeIn(animationSpec = overlayBarsFadeAnimationSpec),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = overlayBarsSlideAnimationSpec,
                ) + fadeOut(animationSpec = overlayBarsFadeAnimationSpec),
            ) {
                VideoPlayerChromeContainer(
                    contentAlignment = Alignment.Center,
                ) {
                    VideoPlayerTimeline(
                        positionMs = displayedPositionMs,
                        durationMs = playbackSnapshot.durationMs,
                        bufferedPositionMs = playbackSnapshot.bufferedPositionMs,
                        isScrubbing = isScrubbing,
                        seekPreviewState = seekPreviewState,
                        seekPreviewPlayer = seekPreviewPlayer,
                        seekPreviewAspectRatio = seekPreviewAspectRatio,
                        onScrubStarted = onScrubStarted,
                        onScrubPositionChange = onScrubPositionChange,
                        onScrubFinished = onScrubFinished,
                        showsRemainingTime = showsRemainingTime,
                        onToggleRemainingTime = onToggleRemainingTime,
                        markerBoundariesMs = markerBoundariesMs,
                        footerContent = {
                            VideoPlayerTimelineToolbar(
                                hasPreviousEpisode = hasPreviousEpisode,
                                hasNextEpisode = hasNextEpisode,
                                playbackSpeedLabel = playbackSpeedLabel,
                                showSubtitles = showSubtitles,
                                onPreviousEpisode = onPreviousEpisode,
                                onNextEpisode = onNextEpisode,
                                onOpenSpeed = onOpenSpeed,
                                onOpenSubtitles = onOpenSubtitles,
                                onRotate = onRotate,
                                onOpenEpisodes = onOpenEpisodes,
                            )
                        },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = transportVisible,
            modifier = Modifier
                .align(Alignment.Center)
                .zIndex(1f),
            enter = fadeIn(animationSpec = overlayCenterFadeAnimationSpec) +
                scaleIn(
                    animationSpec = tween(120),
                    initialScale = 0.94f,
                ),
            exit = fadeOut(animationSpec = overlayCenterFadeAnimationSpec) +
                scaleOut(
                    animationSpec = tween(100),
                    targetScale = 0.97f,
                ),
        ) {
            VideoPlayerChromeContainer(
                modifier = Modifier.fillMaxSize(),
                matchParentHeight = true,
            ) {
                VideoPlayerCenterControls(
                    isPlaying = playbackSnapshot.isPlaying,
                    isLoading = playbackSnapshot.isLoading,
                    seekStepSeconds = seekStepSeconds,
                    onSeekBackward = onSeekBackward,
                    onTogglePlayback = onTogglePlayback,
                    onSeekForward = onSeekForward,
                )
            }
        }

        AnimatedVisibility(
            visible = lockedChromeVisible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn(animationSpec = overlayCenterFadeAnimationSpec) +
                scaleIn(
                    animationSpec = tween(120),
                    initialScale = 0.94f,
                ),
            exit = fadeOut(animationSpec = overlayCenterFadeAnimationSpec) +
                scaleOut(
                    animationSpec = tween(100),
                    targetScale = 0.97f,
                ),
        ) {
            VideoPlayerChromeContainer(
                contentAlignment = Alignment.TopCenter,
            ) {
                VideoPlayerLockedOverlay(
                    onUnlock = onToggleLock,
                    showPictureInPictureButton = showPictureInPictureButton,
                    childWebView = childWebView,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        AnimatedVisibility(
            visible = playbackSnapshot.isLoading && !chromeVisible,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(animationSpec = overlayCenterFadeAnimationSpec),
            exit = fadeOut(animationSpec = overlayCenterFadeAnimationSpec),
        ) {
            LoadingIndicator(
                modifier = Modifier.size(KatariTheme.sizes.iconLarge),
                tone = ContentTone.OnMedia,
            )
        }

        VideoPlayerSeekFeedback(
            feedbackState = seekFeedbackState,
            onDismissed = onSeekFeedbackDismissed,
            modifier = Modifier.fillMaxSize(),
        )

        VideoPlayerSideGestureFeedback(
            feedbackState = sideGestureFeedbackState,
            onDismissed = onSideGestureFeedbackDismissed,
            modifier = Modifier.fillMaxSize(),
        )

        if (!locked) {
            VideoPlayerEpisodesDrawer(
                visible = episodesDrawerVisible,
                episodeListItems = episodeListItems,
                expandedMemberIds = expandedMemberIds,
                currentEpisodeId = currentEpisodeId,
                playbackStateByEpisodeId = playbackStateByEpisodeId,
                sourceAvailable = sourceAvailable,
                onEpisodeClick = onEpisodeSelected,
                onMemberClick = onMemberToggled,
                onDismissRequest = onDismissEpisodes,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(3f),
            )
        }
    }
}

@Composable
internal fun VideoPlayerChromeContainer(
    modifier: Modifier = Modifier,
    matchParentHeight: Boolean = false,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (matchParentHeight) {
                    Modifier.fillMaxHeight()
                } else {
                    Modifier
                },
            ),
        contentAlignment = contentAlignment,
        content = content,
    )
}
