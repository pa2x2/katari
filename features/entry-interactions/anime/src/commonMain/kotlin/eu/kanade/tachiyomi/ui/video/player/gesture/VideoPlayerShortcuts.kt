package eu.kanade.tachiyomi.ui.video.player.gesture

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import app.katari.presentation.kit.shortcut.KeyboardShortcut
import app.katari.presentation.kit.shortcut.ShortcutGroup
import app.katari.presentation.kit.shortcut.ShortcutKeys
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The player's keyboard shortcuts, where the window has them: the keys desktop's input binds, and the ones the
 * controls' tooltips name. They take their keys before a focused control does, so Space never presses the focused
 * button and the arrows never move focus, all but Enter, which presses Skip only when nothing focused uses it. None is
 * taken while a text field is typed in.
 */
internal object VideoPlayerShortcuts {
    /** How far [VolumeUp] and [VolumeDown], and the wheel over a volume control, step the volume, in percent. */
    const val VOLUME_STEP_PERCENT = 5

    val PlayPause: KeyboardShortcut
        @Composable get() = shortcut(
            "player.playPause",
            stringResource(MR.strings.anime_shortcut_play_pause),
            ShortcutKeys.Chord(Key.Spacebar, stringResource(MR.strings.key_space)),
            ShortcutKeys.Chord(Key.K, "K"),
        )

    @Composable
    fun seekBackward(seekStepSeconds: Int) = shortcut(
        "player.seekBackward",
        stringResource(MR.strings.anime_playback_seek_backward, seekStepSeconds),
        ShortcutKeys.Chord(Key.DirectionLeft, "←"),
        ShortcutKeys.Chord(Key.J, "J"),
    )

    @Composable
    fun seekForward(seekStepSeconds: Int) = shortcut(
        "player.seekForward",
        stringResource(MR.strings.anime_playback_seek_forward, seekStepSeconds),
        ShortcutKeys.Chord(Key.DirectionRight, "→"),
        ShortcutKeys.Chord(Key.L, "L"),
    )

    val VolumeUp: KeyboardShortcut
        @Composable get() = shortcut(
            "player.volumeUp",
            stringResource(MR.strings.anime_shortcut_volume_up),
            ShortcutKeys.Chord(Key.DirectionUp, "↑"),
        )

    val VolumeDown: KeyboardShortcut
        @Composable get() = shortcut(
            "player.volumeDown",
            stringResource(MR.strings.anime_shortcut_volume_down),
            ShortcutKeys.Chord(Key.DirectionDown, "↓"),
        )

    val Mute: KeyboardShortcut
        @Composable get() = shortcut(
            "player.mute",
            stringResource(MR.strings.anime_shortcut_mute),
            ShortcutKeys.Chord(Key.M, "M"),
        )

    /** F, beside the window's own F11. */
    val FullScreen: KeyboardShortcut
        @Composable get() = shortcut(
            "player.fullScreen",
            stringResource(MR.strings.keyboard_shortcut_full_screen),
            ShortcutKeys.Chord(Key.F, "F"),
        )

    val Slower: KeyboardShortcut
        @Composable get() = shortcut(
            "player.slower",
            stringResource(MR.strings.anime_shortcut_slower),
            ShortcutKeys.Chord(Key.Comma, "<", shift = true),
        )

    val Faster: KeyboardShortcut
        @Composable get() = shortcut(
            "player.faster",
            stringResource(MR.strings.anime_shortcut_faster),
            ShortcutKeys.Chord(Key.Period, ">", shift = true),
        )

    val PreviousFrame: KeyboardShortcut
        @Composable get() = shortcut(
            "player.previousFrame",
            stringResource(MR.strings.anime_shortcut_previous_frame),
            ShortcutKeys.Chord(Key.Comma, ","),
        )

    val NextFrame: KeyboardShortcut
        @Composable get() = shortcut(
            "player.nextFrame",
            stringResource(MR.strings.anime_shortcut_next_frame),
            ShortcutKeys.Chord(Key.Period, "."),
        )

    val PreviousEpisode: KeyboardShortcut
        @Composable get() = shortcut(
            "player.previousEpisode",
            stringResource(MR.strings.anime_playback_previous_episode),
            ShortcutKeys.Chord(Key.P, "P", shift = true),
        )

    val NextEpisode: KeyboardShortcut
        @Composable get() = shortcut(
            "player.nextEpisode",
            stringResource(MR.strings.anime_playback_next_episode),
            ShortcutKeys.Chord(Key.N, "N", shift = true),
        )

    val Subtitles: KeyboardShortcut
        @Composable get() = shortcut(
            "player.subtitles",
            stringResource(MR.strings.anime_shortcut_subtitles),
            ShortcutKeys.Chord(Key.C, "C"),
        )

    /** I, in the main window and in picture-in-picture alike: into the small window and back out of it. */
    val PictureInPicture: KeyboardShortcut
        @Composable get() = shortcut(
            "player.pictureInPicture",
            stringResource(MR.strings.pref_enable_anime_picture_in_picture),
            ShortcutKeys.Chord(Key.I, "I"),
        )

    val Episodes: KeyboardShortcut
        @Composable get() = shortcut(
            "player.episodes",
            stringResource(MR.strings.action_view_episodes),
            ShortcutKeys.Chord(Key.E, "E"),
        )

    /** Enter goes to the focused control first: Skip takes focus when it shows, so Enter presses it until moved on. */
    val Skip: KeyboardShortcut
        @Composable get() = shortcut(
            "player.skip",
            stringResource(MR.strings.anime_shortcut_skip),
            ShortcutKeys.Chord(Key.Enter, stringResource(MR.strings.key_enter)),
            beforeFocus = false,
        )

    // The zoom keys are the window's interface size keys, which the player takes while it is open

    val ZoomIn: KeyboardShortcut
        @Composable get() = shortcut(
            "player.zoomIn",
            stringResource(MR.strings.anime_shortcut_zoom_in),
            ShortcutKeys.Chord(Key.Equals, "=", ctrl = true),
            ShortcutKeys.Chord(Key.Equals, "+", ctrl = true, shift = true),
            ShortcutKeys.Chord(Key.NumPadAdd, "+", ctrl = true),
        )

    val ZoomOut: KeyboardShortcut
        @Composable get() = shortcut(
            "player.zoomOut",
            stringResource(MR.strings.anime_shortcut_zoom_out),
            ShortcutKeys.Chord(Key.Minus, "−", ctrl = true),
            ShortcutKeys.Chord(Key.NumPadSubtract, "−", ctrl = true),
        )

    val ZoomFit: KeyboardShortcut
        @Composable get() = shortcut(
            "player.zoomFit",
            stringResource(MR.strings.anime_shortcut_zoom_fit),
            ShortcutKeys.Chord(Key.Zero, "0", ctrl = true),
            ShortcutKeys.Chord(Key.NumPad0, "0", ctrl = true),
        )

    // Ctrl and the arrows move a zoomed picture, as the arrows alone seek and set the volume

    val PanLeft: KeyboardShortcut
        @Composable get() = shortcut(
            "player.panLeft",
            stringResource(MR.strings.anime_shortcut_pan_left),
            ShortcutKeys.Chord(Key.DirectionLeft, "←", ctrl = true),
        )

    val PanRight: KeyboardShortcut
        @Composable get() = shortcut(
            "player.panRight",
            stringResource(MR.strings.anime_shortcut_pan_right),
            ShortcutKeys.Chord(Key.DirectionRight, "→", ctrl = true),
        )

    val PanUp: KeyboardShortcut
        @Composable get() = shortcut(
            "player.panUp",
            stringResource(MR.strings.anime_shortcut_pan_up),
            ShortcutKeys.Chord(Key.DirectionUp, "↑", ctrl = true),
        )

    val PanDown: KeyboardShortcut
        @Composable get() = shortcut(
            "player.panDown",
            stringResource(MR.strings.anime_shortcut_pan_down),
            ShortcutKeys.Chord(Key.DirectionDown, "↓", ctrl = true),
        )

    /**
     * The arrows move the subtitles by a step [dx] across and [dy] down, with Shift by a bigger one when [far], while
     * the subtitle editor is open, where they neither seek nor set the volume. A focused control of its panel, such as
     * a slider, keeps them.
     */
    @Composable
    fun moveSubtitles(dx: Int, dy: Int, far: Boolean): KeyboardShortcut {
        val (key, glyph) = when {
            dx < 0 -> Key.DirectionLeft to "←"
            dx > 0 -> Key.DirectionRight to "→"
            dy < 0 -> Key.DirectionUp to "↑"
            else -> Key.DirectionDown to "↓"
        }
        val label = when (key) {
            Key.DirectionLeft ->
                MR.strings.anime_shortcut_subtitles_left to MR.strings.anime_shortcut_subtitles_far_left
            Key.DirectionRight ->
                MR.strings.anime_shortcut_subtitles_right to MR.strings.anime_shortcut_subtitles_far_right
            Key.DirectionUp ->
                MR.strings.anime_shortcut_subtitles_up to MR.strings.anime_shortcut_subtitles_far_up
            else ->
                MR.strings.anime_shortcut_subtitles_down to MR.strings.anime_shortcut_subtitles_far_down
        }.let { (near, further) -> if (far) further else near }
        return shortcut(
            "player.moveSubtitles.$dx.$dy.$far",
            stringResource(label),
            ShortcutKeys.Chord(key, glyph, shift = far),
            beforeFocus = false,
        )
    }

    private fun shortcut(id: String, label: String, vararg keys: ShortcutKeys, beforeFocus: Boolean = true) =
        KeyboardShortcut(
            id = id,
            label = label,
            keys = keys.toList(),
            group = ShortcutGroup.CurrentScreen,
            whileTyping = false,
            beforeFocus = beforeFocus,
        )
}
