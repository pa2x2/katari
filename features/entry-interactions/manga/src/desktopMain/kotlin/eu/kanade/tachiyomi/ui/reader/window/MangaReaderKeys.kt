package eu.kanade.tachiyomi.ui.reader.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import app.katari.presentation.kit.shortcut.ContextMenuKey
import app.katari.presentation.kit.shortcut.KeyboardShortcut
import app.katari.presentation.kit.shortcut.KeyboardShortcutHandler
import app.katari.presentation.kit.shortcut.LocalKeyboardShortcuts
import app.katari.presentation.kit.shortcut.ShortcutGroup
import app.katari.presentation.kit.shortcut.ShortcutKeys
import app.katari.presentation.kit.window.WindowFullScreen
import eu.kanade.tachiyomi.ui.reader.screen.MangaReaderController
import eu.kanade.tachiyomi.ui.reader.viewer.key.ReaderKeyChord
import eu.kanade.tachiyomi.ui.reader.viewer.key.ReaderKeyCommand
import eu.kanade.tachiyomi.ui.reader.viewer.key.chords
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The reader's keys ([ReaderKeyCommand]) as the window's shortcuts, listed on ? beside the window's Ctrl+/, and F for
 * full screen beside the window's F11, as the player has. They take their keys before a focused control, so the
 * arrows never move focus, and leave them while [enabled] is false, as while a sheet or menu of the reader is open.
 * The zoom keys are the window's interface size keys, which the reader takes while it is open.
 */
@Composable
internal fun MangaReaderKeys(controller: MangaReaderController, fullScreen: WindowFullScreen?, enabled: Boolean) {
    ReaderKeyCommand.entries.forEach { command ->
        KeyboardShortcutHandler(shortcut(command), enabled) { controller.onKeyCommand(command) }
    }
    if (fullScreen != null) {
        KeyboardShortcutHandler(
            shortcut(
                "reader.fullScreen",
                stringResource(MR.strings.keyboard_shortcut_full_screen),
                ShortcutKeys.Chord(Key.F, "F"),
            ),
            enabled,
        ) {
            if (fullScreen.isFullScreen) fullScreen.leave() else fullScreen.enter()
        }
    }
    val registry = LocalKeyboardShortcuts.current
    KeyboardShortcutHandler(
        shortcut(
            "reader.shortcuts",
            stringResource(MR.strings.keyboard_shortcut_list),
            ShortcutKeys.Chord(Key.Slash, "?", shift = true),
        ),
    ) {
        registry?.showList()
    }
}

@Composable
private fun shortcut(command: ReaderKeyCommand): KeyboardShortcut {
    val label = when (command) {
        ReaderKeyCommand.PageRight -> stringResource(MR.strings.reader_shortcut_page_right)
        ReaderKeyCommand.PageLeft -> stringResource(MR.strings.reader_shortcut_page_left)
        ReaderKeyCommand.NextPage -> stringResource(MR.strings.reader_shortcut_next_page)
        ReaderKeyCommand.PreviousPage -> stringResource(MR.strings.reader_shortcut_previous_page)
        ReaderKeyCommand.FirstPage -> stringResource(MR.strings.reader_shortcut_first_page)
        ReaderKeyCommand.LastPage -> stringResource(MR.strings.reader_shortcut_last_page)
        ReaderKeyCommand.NextChapter -> stringResource(MR.strings.action_next_chapter)
        ReaderKeyCommand.PreviousChapter -> stringResource(MR.strings.action_previous_chapter)
        ReaderKeyCommand.ToggleMenu -> stringResource(MR.strings.reader_shortcut_menu)
        ReaderKeyCommand.ZoomIn -> stringResource(MR.strings.reader_shortcut_zoom_in)
        ReaderKeyCommand.ZoomOut -> stringResource(MR.strings.reader_shortcut_zoom_out)
        ReaderKeyCommand.ZoomFit -> stringResource(MR.strings.reader_shortcut_zoom_fit)
    }
    val keys = command.chords.map { chord ->
        ShortcutKeys.Chord(chord.key, keyName(chord), ctrl = chord.ctrl, shift = chord.shift)
    }
    return shortcut("reader.${command.name}", label, *keys.toTypedArray())
}

@Composable
private fun keyName(chord: ReaderKeyChord): String = when (chord.key) {
    Key.DirectionRight -> "→"
    Key.DirectionLeft -> "←"
    Key.DirectionDown -> "↓"
    Key.DirectionUp -> "↑"
    Key.PageDown -> stringResource(MR.strings.key_page_down)
    Key.PageUp -> stringResource(MR.strings.key_page_up)
    Key.MoveHome -> stringResource(MR.strings.key_home)
    Key.MoveEnd -> stringResource(MR.strings.key_end)
    ContextMenuKey -> stringResource(MR.strings.key_menu)
    Key.Equals -> if (chord.shift) "+" else "="
    Key.NumPadAdd -> "+"
    Key.Minus, Key.NumPadSubtract -> "−"
    Key.Zero, Key.NumPad0 -> "0"
    Key.N -> "N"
    Key.P -> "P"
    else -> error("No name for ${chord.key}")
}

private fun shortcut(id: String, label: String, vararg keys: ShortcutKeys) = KeyboardShortcut(
    id = id,
    label = label,
    keys = keys.toList(),
    group = ShortcutGroup.CurrentScreen,
    whileTyping = false,
    beforeFocus = true,
)
