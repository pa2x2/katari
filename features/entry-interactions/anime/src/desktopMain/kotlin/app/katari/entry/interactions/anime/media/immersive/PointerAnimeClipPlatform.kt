package app.katari.entry.interactions.anime.media.immersive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import app.katari.core.platform.power.ScreenIdleInhibitor
import app.katari.entry.interactions.settings.AnimePlayerPreferences
import app.katari.entry.viewer.chrome.ui.window.KeepScreenAwake
import app.katari.entry.viewer.settings.ViewerSettingBinder
import app.katari.presentation.kit.shortcut.KeyboardShortcutHandler
import app.katari.video.engine.spi.player.VideoPlayer
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerSeekDirection
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerShortcuts
import eu.kanade.tachiyomi.ui.video.player.surface.VideoSurface
import tachiyomi.domain.entry.model.PictureFraming
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The desktop's feed clips: a click shows or hides the clip's controls, and the current clip takes the player's keys
 * for playing, muting and seeking by the player's seek step, before a focused control does. Moving between clips is
 * the feed's. The screen stays awake while a clip plays in a window that is shown; the sound has no device volume to
 * follow, so the feed stays muted until unmuted.
 */
internal class PointerAnimeClipPlatform(
    private val inhibitor: ScreenIdleInhibitor,
    binder: ViewerSettingBinder,
    preferences: AnimePlayerPreferences,
) : AnimeClipPlatform {
    private val seekStep by lazy { binder.bind(preferences.seekStepSetting) }

    @Composable
    override fun Picture(player: VideoPlayer, aspectRatio: Float?, target: AnimeClipInputTarget, modifier: Modifier) {
        val seekStepSeconds = seekStep.state.collectAsState().value.effectiveValue
        KeyboardShortcutHandler(VideoPlayerShortcuts.PlayPause, enabled = target.active) { target.togglePlayback() }
        KeyboardShortcutHandler(VideoPlayerShortcuts.Mute, enabled = target.active) { target.toggleMute() }
        KeyboardShortcutHandler(VideoPlayerShortcuts.seekBackward(seekStepSeconds), enabled = target.active) {
            target.seek(VideoPlayerSeekDirection.Backward, seekStepSeconds)
        }
        KeyboardShortcutHandler(VideoPlayerShortcuts.seekForward(seekStepSeconds), enabled = target.active) {
            target.seek(VideoPlayerSeekDirection.Forward, seekStepSeconds)
        }
        VideoSurface(
            player = player,
            framing = { PictureFraming.Fit },
            aspectRatio = { aspectRatio },
            modifier = modifier.clipClicks(target),
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

/** A left click that doesn't drag shows or hides the controls as soon as the button is let go. */
private fun Modifier.clipClicks(target: AnimeClipInputTarget): Modifier = pointerInput(target) {
    awaitPointerEventScope {
        var pressedAt: Offset? = null
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull() ?: continue
            when (event.type) {
                PointerEventType.Press -> if (event.button == PointerButton.Primary) pressedAt = change.position
                PointerEventType.Release -> {
                    val at = pressedAt ?: continue
                    if (event.button != PointerButton.Primary) continue
                    pressedAt = null
                    if ((change.position - at).getDistance() <= viewConfiguration.touchSlop) target.toggleControls()
                }
                else -> Unit
            }
        }
    }
}
