package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerEpisodeListEntry
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerSeekFeedbackState
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerSideGestureFeedbackState
import eu.kanade.tachiyomi.ui.video.player.markers.VideoPlayerChapter
import eu.kanade.tachiyomi.ui.video.player.markers.VideoPlayerSkipCountdown
import eu.kanade.tachiyomi.ui.video.player.markers.VideoPlayerSkipOffer
import eu.kanade.tachiyomi.ui.video.player.playback.VideoPlayerPlaybackSnapshot
import eu.kanade.tachiyomi.ui.video.player.posture.VideoPlayerPosture
import eu.kanade.tachiyomi.ui.video.player.posture.currentVideoPlayerPosture
import eu.kanade.tachiyomi.ui.video.player.subtitle.VideoControlsFootprint
import mihon.entry.interactions.anime.introtimes.IntroTimesService
import mihon.entry.interactions.anime.introtimes.OnlineMarker
import mihon.entry.interactions.source.EntryChildWebViewAction
import mihon.entry.interactions.source.EntryChildWebViewResolution
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.entry.model.ChapterGroupId
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.model.PictureFraming

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
    expandedGroupIds: Set<ChapterGroupId>,
    playbackStateByEpisodeId: Map<Long, EntryProgressState>,
    sourceAvailable: Boolean,
    hasPreviousEpisode: Boolean,
    hasNextEpisode: Boolean,
    seekStepSeconds: Int,
    playbackSpeedLabel: String,
    picture: PictureFraming,
    showSubtitles: Boolean,
    seekFeedbackState: VideoPlayerSeekFeedbackState?,
    seekPreviewState: VideoPlayerSeekPreviewState?,
    seekPreviewPicture: (@Composable (Modifier) -> Unit)?,
    seekPreviewAspectRatio: Float?,
    sideGestureFeedbackState: VideoPlayerSideGestureFeedbackState?,
    hideChromeForSeekFeedback: Boolean,
    onSeekFeedbackDismissed: () -> Unit,
    onSideGestureFeedbackDismissed: () -> Unit,
    onBack: () -> Unit,
    showPictureInPictureButton: Boolean,
    onEnterPictureInPicture: () -> Unit,
    /** Locks or unlocks the controls; null where they can't be locked. */
    onToggleLock: (() -> Unit)?,
    onOpenSettings: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onRotate: (() -> Unit)?,
    onTogglePicture: () -> Unit,
    childWebView: EntryChildWebViewResolution.Available?,
    childWebViewActions: List<EntryChildWebViewAction>,
    onChildWebViewAction: (EntryChildWebViewAction, EntryChildWebViewResolution.Available) -> Unit,
    onOpenEpisodes: () -> Unit,
    /** The volume button and slider, where the controls set the volume. */
    volume: (@Composable () -> Unit)?,
    /** Whether the window is in full screen, where it has one, for the full-screen button. */
    fullScreen: Boolean?,
    onToggleFullScreen: () -> Unit,
    onDismissEpisodes: () -> Unit,
    onEpisodeSelected: (EntryChapter) -> Unit,
    onMemberToggled: (Long) -> Unit,
    onGroupToggled: (ChapterGroupId) -> Unit,
    onPreviousEpisode: () -> Unit,
    onSeekBackward: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSeekForward: () -> Unit,
    onNextEpisode: () -> Unit,
    onScrubStarted: () -> Unit,
    onScrubPositionChange: (Long) -> Unit,
    onScrubFinished: () -> Unit,
    swipeSeekDeltaMs: Long?,
    showsRemainingTime: Boolean,
    onToggleRemainingTime: () -> Unit,
    markerBoundariesMs: List<Long>,
    /** The markers found online, whose spans the timeline colours by service. */
    onlineMarkers: List<OnlineMarker>,
    chapter: VideoPlayerChapter?,
    onOpenChapters: () -> Unit,
    skipOffer: VideoPlayerSkipOffer?,
    /** The service the offered skip's marker came from; null for the source's own. */
    skipService: IntroTimesService?,
    skipCountdown: VideoPlayerSkipCountdown?,
    onSkip: () -> Unit,
    onControlsFootprintChange: (VideoControlsFootprint) -> Unit,
    /**
     * Whether a mouse rests on one of the controls, which keeps them shown; the bars around them and the gaps between
     * them don't count. Null where nothing hovers, as on touch screens.
     */
    onPointerOverControlsChange: ((Boolean) -> Unit)?,
    /** Where a mouse hovers over the timeline, null once it leaves; null where nothing hovers. */
    onTimelineHover: ((Long?) -> Unit)?,
    timelineHoverMs: Long?,
    timelineHoverChapter: VideoPlayerChapter?,
    modifier: Modifier = Modifier,
) {
    val chromeVisible = visible && !hideChromeForSeekFeedback && !locked
    // The end screen keeps the top bar for leaving or opening settings; its own Play replaces the transport controls.
    val topBarVisible = chromeVisible || (endScreenVisible && !locked)
    val transportVisible = chromeVisible && !endScreenVisible
    val lockedChromeVisible = visible && !hideChromeForSeekFeedback && locked
    // A marker's first seconds, or its countdown, show the button by itself; after that it comes and goes with the
    // controls.
    val skipVisible = skipOffer != null &&
        !locked &&
        !endScreenVisible &&
        !hideChromeForSeekFeedback &&
        !isScrubbing &&
        (skipOffer.prominent || skipCountdown != null || chromeVisible)

    val posture = currentVideoPlayerPosture()
    // Half-open on a table, the picture keeps to the upper half and everything else to the lower one
    val pictureArea = when (posture) {
        is VideoPlayerPosture.Tabletop -> Modifier.fillMaxWidth().height(posture.pictureBottom)
        else -> Modifier.fillMaxSize()
    }
    val controlsArea = when (posture) {
        is VideoPlayerPosture.Tabletop -> Modifier.fillMaxSize().padding(top = posture.controlsTop)
        else -> Modifier.fillMaxSize()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = controlsArea.zIndex(1f)) {
            VideoPlayerControls(
                posture = posture,
                topBarVisible = topBarVisible,
                transportVisible = transportVisible,
                lockedChromeVisible = lockedChromeVisible,
                skipVisible = skipVisible,
                skipOffer = skipOffer,
                skipService = skipService,
                skipCountdown = skipCountdown,
                onSkip = onSkip,
                onControlsFootprintChange = onControlsFootprintChange,
                onPointerOverControlsChange = onPointerOverControlsChange,
                topBar = {
                    VideoPlayerTopBar(
                        videoTitle = videoTitle,
                        episodeName = episodeName,
                        onBack = onBack,
                        showPictureInPictureButton = showPictureInPictureButton,
                        onEnterPictureInPicture = onEnterPictureInPicture,
                        onToggleLock = onToggleLock,
                        onOpenSettings = onOpenSettings,
                        childWebView = childWebView,
                        childWebViewActions = childWebViewActions,
                        onChildWebViewAction = onChildWebViewAction,
                    )
                },
                timeline = {
                    VideoPlayerTimeline(
                        positionMs = displayedPositionMs,
                        durationMs = playbackSnapshot.durationMs,
                        bufferedPositionMs = playbackSnapshot.bufferedPositionMs,
                        isScrubbing = isScrubbing,
                        seekPreviewState = seekPreviewState,
                        seekPreviewPicture = seekPreviewPicture,
                        seekPreviewAspectRatio = seekPreviewAspectRatio,
                        onScrubStarted = onScrubStarted,
                        onScrubPositionChange = onScrubPositionChange,
                        onScrubFinished = onScrubFinished,
                        showsRemainingTime = showsRemainingTime,
                        onToggleRemainingTime = onToggleRemainingTime,
                        markerBoundariesMs = markerBoundariesMs,
                        onlineMarkers = onlineMarkers,
                        chapter = chapter,
                        onOpenChapters = onOpenChapters,
                        onHover = onTimelineHover,
                        hoverPositionMs = timelineHoverMs,
                        hoverChapter = timelineHoverChapter,
                        timeLabelTrailing = skipOffer?.takeIf { skipVisible }?.let { offer ->
                            {
                                VideoPlayerSkipButton(
                                    offer = offer,
                                    service = skipService,
                                    countdown = skipCountdown,
                                    onClick = onSkip,
                                )
                            }
                        },
                        footerContent = {
                            VideoPlayerTimelineToolbar(
                                hasPreviousEpisode = hasPreviousEpisode,
                                hasNextEpisode = hasNextEpisode,
                                playbackSpeedLabel = playbackSpeedLabel,
                                picture = picture,
                                showSubtitles = showSubtitles,
                                onPreviousEpisode = onPreviousEpisode,
                                onNextEpisode = onNextEpisode,
                                onOpenSpeed = onOpenSpeed,
                                onOpenSubtitles = onOpenSubtitles,
                                onRotate = onRotate,
                                onTogglePicture = onTogglePicture,
                                onOpenEpisodes = onOpenEpisodes,
                                volume = volume,
                                fullScreen = fullScreen,
                                onToggleFullScreen = onToggleFullScreen,
                            )
                        },
                    )
                },
                centre = {
                    // A sideways swipe shows how far it moves in place of the buttons, which a lifted finger could hit.
                    if (swipeSeekDeltaMs != null) {
                        VideoPlayerSwipeSeekDelta(deltaMs = swipeSeekDeltaMs)
                    } else {
                        VideoPlayerCenterControls(
                            isPlaying = playbackSnapshot.isPlaying,
                            isLoading = playbackSnapshot.isLoading,
                            seekStepSeconds = seekStepSeconds,
                            onSeekBackward = onSeekBackward,
                            onTogglePlayback = onTogglePlayback,
                            onSeekForward = onSeekForward,
                        )
                    }
                },
                locked = {
                    // Only controls that can be locked are ever locked.
                    onToggleLock?.let { unlock ->
                        VideoPlayerLockedOverlay(
                            onUnlock = unlock,
                            showPictureInPictureButton = showPictureInPictureButton,
                            childWebView = childWebView,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
            )
        }

        Box(modifier = pictureArea) {
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
        }

        if (!locked) {
            VideoPlayerEpisodesDrawer(
                visible = episodesDrawerVisible,
                episodeListItems = episodeListItems,
                expandedMemberIds = expandedMemberIds,
                expandedGroupIds = expandedGroupIds,
                currentEpisodeId = currentEpisodeId,
                playbackStateByEpisodeId = playbackStateByEpisodeId,
                sourceAvailable = sourceAvailable,
                onEpisodeClick = onEpisodeSelected,
                onMemberClick = onMemberToggled,
                onGroupClick = onGroupToggled,
                onDismissRequest = onDismissEpisodes,
                modifier = controlsArea.zIndex(3f),
            )
        }
    }
}

/**
 * The bars, the centre buttons and the locked controls, laid out in the area the [posture] leaves them. Book posture
 * keeps the centre buttons in the start half, off the crease, and tabletop stacks them between the bars.
 */
@Composable
private fun BoxScope.VideoPlayerControls(
    posture: VideoPlayerPosture,
    topBarVisible: Boolean,
    transportVisible: Boolean,
    lockedChromeVisible: Boolean,
    skipVisible: Boolean,
    skipOffer: VideoPlayerSkipOffer?,
    skipService: IntroTimesService?,
    skipCountdown: VideoPlayerSkipCountdown?,
    onSkip: () -> Unit,
    onControlsFootprintChange: (VideoControlsFootprint) -> Unit,
    onPointerOverControlsChange: ((Boolean) -> Unit)?,
    topBar: @Composable () -> Unit,
    timeline: @Composable () -> Unit,
    centre: @Composable () -> Unit,
    locked: @Composable () -> Unit,
) {
    var bottomRowHeight by remember { mutableIntStateOf(0) }
    var centreButtonsBottom by remember { mutableIntStateOf(0) }
    // Half-open on a table the controls sit under the picture, so the subtitles have nothing to clear
    val footprint = if (transportVisible && posture !is VideoPlayerPosture.Tabletop) {
        VideoControlsFootprint(
            bottomRowTopPx = bottomRowHeight.toFloat(),
            centreButtonsBottomPx = centreButtonsBottom.toFloat(),
        )
    } else {
        VideoControlsFootprint.None
    }
    LaunchedEffect(footprint) { onControlsFootprintChange(footprint) }
    val controlHover = remember { MutableInteractionSource() }
    val pointerOverControls by controlHover.collectIsHoveredAsState()
    LaunchedEffect(pointerOverControls) { onPointerOverControlsChange?.invoke(pointerOverControls) }
    CompositionLocalProvider(
        LocalVideoPlayerControlHover provides controlHover.takeIf { onPointerOverControlsChange != null },
    ) {
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
                VideoPlayerChromeContainer(contentAlignment = Alignment.TopCenter) {
                    topBar()
                }
            }

            // Half-open on a table the centre buttons sit between the bars rather than over the picture
            if (posture is VideoPlayerPosture.Tabletop) {
                VideoPlayerCentreButtons(
                    visible = transportVisible,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    centre = centre,
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // With the controls hidden the button stands alone at the bottom; with them it joins the time label's row.
            AnimatedVisibility(
                visible = skipVisible && !transportVisible,
                modifier = Modifier.align(Alignment.End),
                enter = fadeIn(animationSpec = overlayBarsFadeAnimationSpec),
                exit = fadeOut(animationSpec = overlayBarsFadeAnimationSpec),
            ) {
                skipOffer?.let { offer ->
                    VideoPlayerSkipButton(
                        offer = offer,
                        service = skipService,
                        countdown = skipCountdown,
                        onClick = onSkip,
                        modifier = Modifier
                            .videoPlayerSafeContentPadding(includeBottom = true)
                            .padding(end = KatariTheme.spacing.l, bottom = KatariTheme.spacing.l),
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
                    modifier = Modifier.onSizeChanged { bottomRowHeight = it.height },
                    contentAlignment = Alignment.Center,
                ) {
                    timeline()
                }
            }
        }

        if (posture !is VideoPlayerPosture.Tabletop) {
            VideoPlayerCentreButtons(
                visible = transportVisible,
                modifier = when (posture) {
                    is VideoPlayerPosture.Book -> Modifier.align(Alignment.CenterStart).width(posture.startHalfWidth)
                    else -> Modifier.align(Alignment.Center)
                }.zIndex(1f),
                onBottomChange = { centreButtonsBottom = it },
                centre = centre,
            )
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
                locked()
            }
        }
    }
}

@Composable
private fun VideoPlayerCentreButtons(
    visible: Boolean,
    modifier: Modifier,
    onBottomChange: (Int) -> Unit = {},
    centre: @Composable () -> Unit,
) {
    var areaHeight by remember { mutableIntStateOf(0) }
    var buttonsHeight by remember { mutableIntStateOf(0) }
    // The area centres the buttons in it, so their bottom edge is half the room they leave above the area's bottom
    LaunchedEffect(areaHeight, buttonsHeight) { onBottomChange((areaHeight - buttonsHeight) / 2) }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
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
            modifier = Modifier.fillMaxSize().onSizeChanged { areaHeight = it.height },
            matchParentHeight = true,
        ) {
            Box(modifier = Modifier.onSizeChanged { buttonsHeight = it.height }) { centre() }
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
