package eu.kanade.tachiyomi.ui.video.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import app.katari.core.platform.clipboard.TextClipboard
import app.katari.core.platform.currentPlatformContext
import app.katari.core.platform.link.LinkOpener
import app.katari.core.platform.power.ScreenIdleInhibitor
import app.katari.entry.interactions.player.AnimePlayer
import app.katari.entry.interactions.player.AnimePlayerMediaControls
import app.katari.entry.interactions.player.AnimePlayerNavigation
import app.katari.entry.interactions.player.AnimePlayerPictureInPictureWindow
import app.katari.entry.interactions.player.AnimePlayerPlacement
import app.katari.entry.interactions.player.AnimePlayerRequest
import app.katari.entry.interactions.settings.AnimePlayerDevicePreferences
import app.katari.entry.interactions.settings.AnimePlayerPreferences
import app.katari.entry.interactions.source.EntryChildWebViewAction
import app.katari.entry.viewer.settings.ViewerSettingBinder
import app.katari.presentation.kit.window.LocalWindowFullScreen
import app.katari.presentation.kit.window.ReportScreenTitle
import app.katari.video.engine.spi.VideoEngine
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.tachiyomi.ui.video.player.components.videoPlayerWindowTitle
import eu.kanade.tachiyomi.ui.video.player.engine.VideoPlayerEngines
import eu.kanade.tachiyomi.ui.video.player.gesture.PointerVideoPlayerInput
import eu.kanade.tachiyomi.ui.video.player.mediacontrols.DesktopMediaControls
import eu.kanade.tachiyomi.ui.video.player.pip.WindowPictureInPicture
import eu.kanade.tachiyomi.ui.video.player.playback.VideoPlayerPlaybackController
import eu.kanade.tachiyomi.ui.video.player.playback.holdExitUntilDone
import eu.kanade.tachiyomi.ui.video.player.recovery.VideoProblemAction
import eu.kanade.tachiyomi.ui.video.player.screen.VideoPlayerScreen
import eu.kanade.tachiyomi.ui.video.player.screen.VideoPlayerScreenHost
import eu.kanade.tachiyomi.ui.video.player.screen.VideoPlayerSettingBindings
import eu.kanade.tachiyomi.ui.video.player.screen.VideoPlayerVisitState
import eu.kanade.tachiyomi.ui.video.player.surface.ComposeSeekPreviewSurface
import eu.kanade.tachiyomi.ui.video.player.volume.PlayerOwnVolume
import eu.kanade.tachiyomi.ui.video.player.window.VideoPlayerFullScreen
import eu.kanade.tachiyomi.ui.video.player.window.VideoPlayerKeepAwake
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The desktop's [AnimePlayer]. It plays in mpv, the desktop's one engine, takes the mouse on its picture and the
 * keyboard ([PointerVideoPlayerInput]), sets the player's own volume, offers the window's full screen and moves into a
 * picture-in-picture window of its own ([WindowPictureInPicture]) and shows in the desktop's media controls
 * ([DesktopMediaControls]) while it is open; there is no lock, rotation or brightness. Like Android's player activity it
 * owns what outlives each episode's composition, and the screen itself is the shared [VideoPlayerScreen]. While a screen it opened covers it, it stays composed and paused.
 * The main window's title names the series and episode while the player fills it.
 */
@Inject
internal class DesktopVideoPlayer(
    private val videoEngine: VideoEngine,
    private val preferences: AnimePlayerPreferences,
    private val devicePreferences: AnimePlayerDevicePreferences,
    private val viewerSettingBinder: ViewerSettingBinder,
    private val clipboard: TextClipboard,
    private val linkOpener: LinkOpener,
    private val screenIdleInhibitor: ScreenIdleInhibitor,
    private val pictureInPictureWindow: AnimePlayerPictureInPictureWindow,
    private val systemMediaControls: AnimePlayerMediaControls,
) : AnimePlayer {
    @Composable
    override fun Content(
        request: AnimePlayerRequest,
        navigation: AnimePlayerNavigation,
        placement: AnimePlayerPlacement,
        mainWindowHidden: StateFlow<Boolean>,
    ) {
        val covered = placement == AnimePlayerPlacement.Covered
        val viewModel = metroViewModel<VideoPlayerViewModel>()
        val settings = remember { VideoPlayerSettingBindings(viewerSettingBinder, preferences) }
        remember(viewModel) {
            viewModel.init(
                entryId = request.entryId,
                chapterId = request.episodeId,
                ownerEntryId = request.ownerEntryId,
                bypassMerge = request.bypassMerge,
                startPositionMs = request.startPositionMs,
                defaultPlaybackSpeed = settings.defaultPlaybackSpeedPercent.resolveProfile().effectiveValue / 100f,
            )
        }
        val scope = rememberCoroutineScope()
        val playback = remember(viewModel) {
            VideoPlayerPlaybackController(scope) { positionMs, durationMs, activeDurationMs, settled ->
                viewModel.persistPlayback(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    activeDurationMs = activeDurationMs,
                    settled = settled,
                )
            }
        }
        // Closing the window quits the app, whose process ends soon after the player lets go of the episode
        DisposableEffect(playback) {
            onDispose {
                playback.release(persistState = true)
                viewModel.playbackPersistence?.let(::holdExitUntilDone)
            }
        }
        LaunchedEffect(playback, covered) {
            if (covered) {
                playback.player?.pause()
                playback.flush()
            }
        }
        val context = currentPlatformContext()
        val latestNavigation by rememberUpdatedState(navigation)
        val latestPlacement by rememberUpdatedState(placement)
        LaunchedEffect(playback, mainWindowHidden) {
            mainWindowHidden.collect { hidden ->
                if (hidden && latestPlacement != AnimePlayerPlacement.PictureInPicture) {
                    playback.player?.pause()
                    playback.flush()
                }
            }
        }
        val pictureInPicture = remember {
            WindowPictureInPicture(pictureInPictureWindow, navigation = { latestNavigation }) {
                latestPlacement == AnimePlayerPlacement.PictureInPicture
            }
        }
        val mediaControls = remember { DesktopMediaControls(systemMediaControls, scope) { latestNavigation } }
        DisposableEffect(mediaControls) {
            onDispose(mediaControls::release)
        }
        val fullScreen = LocalWindowFullScreen.current
        val host = remember(playback, fullScreen) {
            val volume = PlayerOwnVolume(devicePreferences)
            VideoPlayerScreenHost(
                engines = VideoPlayerEngines(builtIn = videoEngine, mpvAddon = null),
                preferences = preferences,
                settings = settings,
                visit = VideoPlayerVisitState(
                    gestureGuideVisible = !preferences.gestureGuideShown.get(),
                    showsRemainingTime = preferences.showsRemainingTime.get(),
                    subtitlesInPicture = preferences.subtitlesInPicture.get(),
                ),
                playback = playback,
                input = PointerVideoPlayerInput,
                seekPreviewSurface = ComposeSeekPreviewSurface,
                pictureInPicture = pictureInPicture,
                mediaControls = mediaControls,
                volume = volume,
                volumeControl = volume,
                fullScreen = fullScreen,
                brightness = null,
                orientation = null,
                locksControls = false,
                clipboard = clipboard,
                webViewActions = buildList {
                    if (navigation.webViewAvailable) add(EntryChildWebViewAction.OPEN_IN_WEB_VIEW)
                    add(EntryChildWebViewAction.OPEN_IN_BROWSER)
                },
                onClose = { latestNavigation.close() },
                onHideSystemUi = {},
                onOpenDefaultSettings = { latestNavigation.openPlayerSettings() },
                onProblemAction = { action ->
                    when (action) {
                        VideoProblemAction.OpenExtensions -> latestNavigation.openExtensions()
                        is VideoProblemAction.OpenEntry -> latestNavigation.openEntry(action.entryId)
                        is VideoProblemAction.OpenSourceSettings -> latestNavigation.openSourceSettings(action.sourceId)
                    }
                },
                onOpenWebView = { action, resolution, title ->
                    when (action) {
                        EntryChildWebViewAction.OPEN_IN_WEB_VIEW ->
                            latestNavigation.openWebView(resolution.url, title, resolution.sourceId)
                        EntryChildWebViewAction.OPEN_IN_BROWSER -> linkOpener.open(context, resolution.url)
                        EntryChildWebViewAction.SHARE -> error("The desktop player offers no sharing")
                    }
                },
            )
        }
        WindowTitle(viewModel, shown = placement == AnimePlayerPlacement.Window)
        VideoPlayerKeepAwake(
            playback = playback,
            inhibitor = screenIdleInhibitor,
            inPictureInPicture = placement == AnimePlayerPlacement.PictureInPicture,
        )
        VideoPlayerFullScreen(fullScreen, inPictureInPicture = placement == AnimePlayerPlacement.PictureInPicture) {
            VideoPlayerScreen(viewModel = viewModel, host = host)
        }
    }
}

// In picture-in-picture the small window names the episode, and the main window the screens it shows
@Composable
private fun WindowTitle(viewModel: VideoPlayerViewModel, shown: Boolean) {
    val state by viewModel.state.collectAsState()
    ReportScreenTitle(
        when (val current = state) {
            is VideoPlayerViewModel.State.Ready ->
                videoPlayerWindowTitle(current.entryTitle, current.memberTitle, current.chapterName)
            is VideoPlayerViewModel.State.Loading ->
                current.title?.let { videoPlayerWindowTitle(it, current.memberTitle, current.episodeName) }
            is VideoPlayerViewModel.State.Error ->
                current.title?.let { videoPlayerWindowTitle(it, current.memberTitle, current.episodeName) }
        }?.takeIf { shown },
    )
}
