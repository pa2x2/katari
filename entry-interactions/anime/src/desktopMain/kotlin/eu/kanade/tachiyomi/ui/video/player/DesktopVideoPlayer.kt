package eu.kanade.tachiyomi.ui.video.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.tachiyomi.ui.video.player.components.videoPlayerEpisodeLine
import eu.kanade.tachiyomi.ui.video.player.engine.VideoPlayerEngines
import eu.kanade.tachiyomi.ui.video.player.gesture.PointerVideoPlayerInput
import eu.kanade.tachiyomi.ui.video.player.mediacontrols.NoMediaControls
import eu.kanade.tachiyomi.ui.video.player.pip.NoPictureInPicture
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
import mihon.core.platform.clipboard.TextClipboard
import mihon.core.platform.currentPlatformContext
import mihon.core.platform.link.LinkOpener
import mihon.core.platform.power.ScreenIdleInhibitor
import mihon.entry.interactions.player.AnimePlayer
import mihon.entry.interactions.player.AnimePlayerNavigation
import mihon.entry.interactions.player.AnimePlayerRequest
import mihon.entry.interactions.settings.AnimePlayerDevicePreferences
import mihon.entry.interactions.settings.AnimePlayerPreferences
import mihon.entry.interactions.source.EntryChildWebViewAction
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.presentation.kit.window.LocalWindowFullScreen
import mihon.presentation.kit.window.ReportScreenTitle
import mihon.video.engine.spi.VideoEngine
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The desktop's [AnimePlayer]. It plays in mpv, the desktop's one engine, takes the mouse on its picture and the
 * keyboard ([PointerVideoPlayerInput]), sets the player's own volume and offers the window's full screen; there is no
 * lock, rotation, brightness, picture-in-picture or media controls outside the window. Like Android's player activity
 * it owns what outlives each episode's composition, and the screen itself is the shared [VideoPlayerScreen]. While a
 * screen it opened covers it, it stays composed and paused. The window's title names the series and episode while it
 * shows.
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
) : AnimePlayer {
    @Composable
    override fun Content(request: AnimePlayerRequest, navigation: AnimePlayerNavigation, covered: Boolean) {
        val viewModel = metroViewModel<VideoPlayerViewModel>()
        val settings = remember { VideoPlayerSettingBindings(viewerSettingBinder, preferences) }
        remember(viewModel) {
            viewModel.init(
                entryId = request.entryId,
                chapterId = request.episodeId,
                ownerEntryId = request.ownerEntryId,
                bypassMerge = request.bypassMerge,
                defaultPlaybackSpeed = settings.defaultPlaybackSpeedPercent.resolveProfile().effectiveValue / 100f,
            )
        }
        val scope = rememberCoroutineScope()
        val playback = remember(viewModel) {
            VideoPlayerPlaybackController(scope) { positionMs, durationMs, activeDurationMs ->
                viewModel.persistPlayback(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    activeDurationMs = activeDurationMs,
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
                pictureInPicture = NoPictureInPicture,
                mediaControls = NoMediaControls,
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
        WindowTitle(viewModel)
        VideoPlayerKeepAwake(playback, screenIdleInhibitor)
        VideoPlayerFullScreen(fullScreen) {
            VideoPlayerScreen(viewModel = viewModel, host = host)
        }
    }
}

@Composable
private fun WindowTitle(viewModel: VideoPlayerViewModel) {
    val state by viewModel.state.collectAsState()
    val (series, episode) = when (val current = state) {
        is VideoPlayerViewModel.State.Ready ->
            current.entryTitle to videoPlayerEpisodeLine(current.memberTitle, current.chapterName)
        is VideoPlayerViewModel.State.Loading ->
            current.title to videoPlayerEpisodeLine(current.memberTitle, current.episodeName)
        is VideoPlayerViewModel.State.Error -> null to ""
    }
    ReportScreenTitle(
        when {
            series == null -> null
            episode.isEmpty() -> series
            else -> stringResource(MR.strings.anime_playback_window_title, series, episode)
        },
    )
}
