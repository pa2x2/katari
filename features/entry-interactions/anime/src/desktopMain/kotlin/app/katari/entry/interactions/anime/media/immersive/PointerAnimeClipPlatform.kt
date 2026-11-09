package app.katari.entry.interactions.anime.media.immersive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import app.katari.core.platform.power.ScreenIdleInhibitor
import app.katari.entry.interactions.settings.AnimePlayerDevicePreferences
import app.katari.entry.interactions.settings.AnimePlayerPreferences
import app.katari.entry.viewer.chrome.ui.window.KeepScreenAwake
import app.katari.entry.viewer.settings.ViewerSettingBinder
import app.katari.presentation.kit.shortcut.KeyboardShortcut
import app.katari.presentation.kit.shortcut.KeyboardShortcutHandler
import app.katari.presentation.kit.shortcut.ShortcutGroup
import app.katari.presentation.kit.shortcut.ShortcutKeys
import app.katari.presentation.kit.window.LocalWindowFullScreen
import app.katari.presentation.kit.window.WindowFullScreen
import app.katari.video.engine.spi.player.VideoPlayer
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerSeekDirection
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerShortcuts
import eu.kanade.tachiyomi.ui.video.player.surface.VideoSurface
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.core.common.preference.Preference
import tachiyomi.domain.entry.model.PictureFraming
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The desktop's feed clips, worked with the mouse as the player is: a click plays or pauses, a double-click switches
 * full screen, and holding the left button plays at 2× until it is let go. The feed shows and hides the controls as the
 * mouse moves and rests. The current clip takes the player's keys for playing, muting and seeking by the player's seek
 * step, before a focused control does, and Shift with ↑ or ↓ for the volume, as ↑ and ↓ alone move between clips, which
 * is the feed's. Clips play at the player's own volume. The screen stays awake while a clip plays in a window that is
 * shown.
 */
internal class PointerAnimeClipPlatform(
    private val inhibitor: ScreenIdleInhibitor,
    binder: ViewerSettingBinder,
    preferences: AnimePlayerPreferences,
    devicePreferences: AnimePlayerDevicePreferences,
) : AnimeClipPlatform {
    private val seekStep by lazy { binder.bind(preferences.seekStepSetting) }

    override val controlsHideWhilePlaying = false

    override val playerVolume: Preference<Int> = devicePreferences.volumePercent

    @Composable
    override fun Picture(player: VideoPlayer, aspectRatio: Float?, target: AnimeClipInputTarget, modifier: Modifier) {
        val seekStepSeconds = seekStep.state.collectAsState().value.effectiveValue
        val fullScreen = LocalWindowFullScreen.current
        KeyboardShortcutHandler(VideoPlayerShortcuts.PlayPause, enabled = target.active) { target.togglePlayback() }
        KeyboardShortcutHandler(VideoPlayerShortcuts.Mute, enabled = target.active) { target.toggleMute() }
        KeyboardShortcutHandler(VideoPlayerShortcuts.seekBackward(seekStepSeconds), enabled = target.active) {
            target.seek(VideoPlayerSeekDirection.Backward, seekStepSeconds)
        }
        KeyboardShortcutHandler(VideoPlayerShortcuts.seekForward(seekStepSeconds), enabled = target.active) {
            target.seek(VideoPlayerSeekDirection.Forward, seekStepSeconds)
        }
        KeyboardShortcutHandler(volumeShortcut(up = true), enabled = target.active) {
            target.stepVolume(VideoPlayerShortcuts.VOLUME_STEP_PERCENT)
        }
        KeyboardShortcutHandler(volumeShortcut(up = false), enabled = target.active) {
            target.stepVolume(-VideoPlayerShortcuts.VOLUME_STEP_PERCENT)
        }
        VideoSurface(
            player = player,
            framing = { PictureFraming.Fit },
            aspectRatio = { aspectRatio },
            modifier = modifier.clipMouse(target, fullScreen),
        )
    }

    @Composable
    override fun KeepAwake(playing: Boolean) = KeepScreenAwake(
        active = playing,
        reason = stringResource(MR.strings.anime_playback_keeps_screen_awake),
        inhibitor = inhibitor,
    )

    @Composable
    override fun UnmuteOnVolumeRaise(active: Boolean, muted: Boolean, onUnmute: () -> Unit) = Unit
}

/**
 * The left button on a clip, as on the player's picture: a click plays or pauses as soon as the button is let go, and
 * the second click of a double-click plays or pauses back and switches [fullScreen], so a double-click leaves playback
 * as it was. Holding the button plays at 2× until it is let go; a press that moves is neither.
 */
private fun Modifier.clipMouse(target: AnimeClipInputTarget, fullScreen: WindowFullScreen?): Modifier =
    pointerInput(target, fullScreen) {
        coroutineScope {
            var lastClickAtMs: Long? = null
            var lastClickAt = Offset.Zero
            var press: ClipPress? = null
            var hold: Job? = null
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull() ?: continue
                    val current = press
                    when (event.type) {
                        PointerEventType.Move -> {
                            if (current == null || current.boosted || !current.clicks) continue
                            if ((change.position - current.at).getDistance() > viewConfiguration.touchSlop) {
                                current.clicks = false
                                hold?.cancel()
                            }
                        }
                        PointerEventType.Press -> {
                            if (event.button != PointerButton.Primary) continue
                            val sinceClickMs = lastClickAtMs?.let { change.uptimeMillis - it }
                            val double = sinceClickMs != null &&
                                sinceClickMs <= viewConfiguration.doubleTapTimeoutMillis &&
                                (change.position - lastClickAt).getDistance() <= viewConfiguration.touchSlop
                            if (double) {
                                lastClickAtMs = null
                                press = ClipPress(change.position, clicks = false)
                                target.togglePlayback()
                                if (fullScreen != null) {
                                    if (fullScreen.isFullScreen) fullScreen.leave() else fullScreen.enter()
                                }
                                continue
                            }
                            val started = ClipPress(change.position, clicks = true)
                            press = started
                            hold = launch {
                                delay(viewConfiguration.longPressTimeoutMillis)
                                started.clicks = false
                                started.boosted = true
                                target.setSpeedBoost(true)
                            }
                        }
                        PointerEventType.Release -> {
                            if (event.button != PointerButton.Primary || current == null) continue
                            press = null
                            hold?.cancel()
                            when {
                                current.boosted -> target.setSpeedBoost(false)
                                current.clicks -> {
                                    lastClickAtMs = change.uptimeMillis
                                    lastClickAt = change.position
                                    target.togglePlayback()
                                }
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

@Composable
private fun volumeShortcut(up: Boolean) = KeyboardShortcut(
    id = if (up) "immersive.volumeUp" else "immersive.volumeDown",
    label = stringResource(if (up) MR.strings.anime_shortcut_volume_up else MR.strings.anime_shortcut_volume_down),
    keys = listOf(
        ShortcutKeys.Chord(if (up) Key.DirectionUp else Key.DirectionDown, if (up) "↑" else "↓", shift = true),
    ),
    group = ShortcutGroup.CurrentScreen,
    whileTyping = false,
    beforeFocus = true,
)

// As the player's ↑ and ↓ step it

/** A left button press on a clip, until it is let go. */
private class ClipPress(val at: Offset, var clicks: Boolean) {
    var boosted = false
}
