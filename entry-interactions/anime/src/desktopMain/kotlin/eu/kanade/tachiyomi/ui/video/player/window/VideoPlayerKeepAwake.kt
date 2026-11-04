package eu.kanade.tachiyomi.ui.video.player.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.awt.LocalAwtWindow
import eu.kanade.tachiyomi.ui.video.player.playback.VideoPlayerPlaybackController
import kotlinx.coroutines.delay
import mihon.core.platform.currentPlatformContext
import mihon.core.platform.power.ScreenIdleInhibitor
import mihon.video.engine.spi.player.VideoPlayerState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.awt.Frame
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowEvent
import java.awt.event.WindowStateListener

/**
 * Keeps the screen awake while the watched episode plays, or waits to play, where it is seen: in the main window while
 * that is shown, or in picture-in-picture ([inPictureInPicture]), whatever the main window does. A minimised or hidden
 * main window with no picture-in-picture lets the screen lock while the sound plays on, as nobody is watching it. The
 * screen is let go of only once that has lasted a moment, as switching to full screen hides the window for an instant
 * and an episode change stops playing for one.
 */
@Composable
internal fun VideoPlayerKeepAwake(
    playback: VideoPlayerPlaybackController,
    inhibitor: ScreenIdleInhibitor,
    inPictureInPicture: Boolean,
) {
    val window = LocalAwtWindow.current ?: return
    val context = currentPlatformContext()
    val player = playback.player
    val playing by produceState(false, player) {
        value = false
        player?.state?.collect { value = it.meansToPlay }
    }
    val shown by rememberWindowShown(window)
    val reason = stringResource(MR.strings.anime_playback_keeps_screen_awake)
    val keepsAwake = playing && (shown || inPictureInPicture)
    val hold = remember { ScreenHold() }
    LaunchedEffect(keepsAwake) {
        if (keepsAwake) {
            if (hold.current == null) hold.current = inhibitor.keepAwake(context, reason)
        } else {
            delay(RELEASE_DELAY_MS)
            hold.release()
        }
    }
    DisposableEffect(hold) {
        onDispose(hold::release)
    }
}

private class ScreenHold {
    var current: AutoCloseable? = null

    fun release() {
        current?.close()
        current = null
    }
}

private const val RELEASE_DELAY_MS = 1_500L

// Buffering counts, so a stall doesn't let go of the screen only to take it again a moment later
private val VideoPlayerState.meansToPlay: Boolean
    get() = playWhenReady && when (playback) {
        VideoPlayerState.Playback.Ready, VideoPlayerState.Playback.Buffering -> true
        VideoPlayerState.Playback.Idle, VideoPlayerState.Playback.Ended -> false
    }

@Composable
private fun rememberWindowShown(window: Window) = remember(window) { mutableStateOf(window.isShownOnScreen) }
    .also { shown ->
        DisposableEffect(window) {
            val stateListener = WindowStateListener { _: WindowEvent -> shown.value = window.isShownOnScreen }
            val componentListener = object : ComponentAdapter() {
                override fun componentShown(event: ComponentEvent) {
                    shown.value = window.isShownOnScreen
                }

                override fun componentHidden(event: ComponentEvent) {
                    shown.value = window.isShownOnScreen
                }
            }
            window.addWindowStateListener(stateListener)
            window.addComponentListener(componentListener)
            onDispose {
                window.removeWindowStateListener(stateListener)
                window.removeComponentListener(componentListener)
            }
        }
    }

private val Window.isShownOnScreen: Boolean
    get() = isShowing && (this !is Frame || extendedState and Frame.ICONIFIED == 0)
