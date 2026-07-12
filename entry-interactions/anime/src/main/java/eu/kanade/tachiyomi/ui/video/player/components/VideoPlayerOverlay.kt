package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.media3.exoplayer.ExoPlayer
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerEpisodeListEntry
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerPlaybackSnapshot
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerSeekFeedbackState
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerSeekPreviewState
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.PlaybackState

private val overlayBarsSlideAnimationSpec = tween<IntOffset>(190)
private val overlayBarsFadeAnimationSpec = tween<Float>(125)
private val overlayCenterFadeAnimationSpec = tween<Float>(110)

@Composable
internal fun VideoPlayerOverlay(
    visible: Boolean,
    locked: Boolean,
    videoTitle: String,
    episodeName: String,
    playbackSnapshot: VideoPlayerPlaybackSnapshot,
    displayedPositionMs: Long,
    isScrubbing: Boolean,
    episodesDrawerVisible: Boolean,
    anime: Entry,
    currentEpisodeId: Long,
    episodeListItems: List<VideoPlayerEpisodeListEntry>,
    playbackStateByEpisodeId: Map<Long, PlaybackState>,
    sourceAvailable: Boolean,
    hasPreviousEpisode: Boolean,
    hasNextEpisode: Boolean,
    seekFeedbackState: VideoPlayerSeekFeedbackState?,
    seekPreviewState: VideoPlayerSeekPreviewState?,
    seekPreviewPlayer: ExoPlayer?,
    seekPreviewAspectRatio: Float?,
    sideGestureFeedbackState: eu.kanade.tachiyomi.ui.video.player.VideoPlayerSideGestureFeedbackState?,
    hideChromeForSeekFeedback: Boolean,
    onSeekFeedbackDismissed: () -> Unit,
    onSideGestureFeedbackDismissed: () -> Unit,
    onBack: () -> Unit,
    showPictureInPictureButton: Boolean,
    onEnterPictureInPicture: () -> Unit,
    onToggleLock: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenEpisodes: () -> Unit,
    onDismissEpisodes: () -> Unit,
    onEpisodeSelected: (EntryChapter) -> Unit,
    onPreviousEpisode: () -> Unit,
    onSeekBackward: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSeekForward: () -> Unit,
    onNextEpisode: () -> Unit,
    onScrubStarted: () -> Unit,
    onScrubPositionChange: (Long) -> Unit,
    onScrubFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chromeVisible = visible && !hideChromeForSeekFeedback && !locked
    val lockedChromeVisible = visible && !hideChromeForSeekFeedback && locked

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f),
        ) {
            AnimatedVisibility(
                visible = chromeVisible,
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
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            AnimatedVisibility(
                visible = chromeVisible,
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
                        footerContent = {
                            VideoPlayerTimelineToolbar(onOpenEpisodes = onOpenEpisodes)
                        },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = chromeVisible,
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
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.24f),
                                Color.Black.copy(alpha = 0.1f),
                                Color.Transparent,
                            ),
                            radius = 380f,
                        ),
                    )
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                VideoPlayerChromeContainer(
                    modifier = Modifier.fillMaxSize(),
                    matchParentHeight = true,
                ) {
                    VideoPlayerCenterControls(
                        isPlaying = playbackSnapshot.isPlaying,
                        isLoading = playbackSnapshot.isLoading,
                        hasPreviousEpisode = hasPreviousEpisode,
                        hasNextEpisode = hasNextEpisode,
                        onPreviousEpisode = onPreviousEpisode,
                        onSeekBackward = onSeekBackward,
                        onTogglePlayback = onTogglePlayback,
                        onSeekForward = onSeekForward,
                        onNextEpisode = onNextEpisode,
                        modifier = Modifier.background(Color.Transparent),
                    )
                }
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
            CircularProgressIndicator(
                modifier = Modifier.size(30.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        }

        AnimatedVisibility(
            visible = chromeVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(animationSpec = overlayBarsFadeAnimationSpec),
            exit = fadeOut(animationSpec = overlayBarsFadeAnimationSpec),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.03f),
                                Color.Black.copy(alpha = 0.18f),
                            ),
                        ),
                    ),
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
                anime = anime,
                episodeListItems = episodeListItems,
                currentEpisodeId = currentEpisodeId,
                playbackStateByEpisodeId = playbackStateByEpisodeId,
                sourceAvailable = sourceAvailable,
                onEpisodeClick = onEpisodeSelected,
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
