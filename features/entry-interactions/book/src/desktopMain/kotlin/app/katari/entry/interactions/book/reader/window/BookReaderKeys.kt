package app.katari.entry.interactions.book.reader.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import app.katari.entry.interactions.book.document.reader.BookDocumentReaderController
import app.katari.entry.interactions.book.document.reader.input.BookDocumentKeyChord
import app.katari.entry.interactions.book.document.reader.input.BookDocumentKeyCommand
import app.katari.entry.interactions.book.document.reader.input.chords
import app.katari.presentation.kit.shortcut.ContextMenuKey
import app.katari.presentation.kit.shortcut.KeyboardShortcut
import app.katari.presentation.kit.shortcut.KeyboardShortcutHandler
import app.katari.presentation.kit.shortcut.LocalKeyboardShortcuts
import app.katari.presentation.kit.shortcut.ScreenShortcuts
import app.katari.presentation.kit.shortcut.ShortcutGroup
import app.katari.presentation.kit.shortcut.ShortcutKeys
import app.katari.presentation.kit.window.WindowFullScreen
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The book reader's keys ([BookDocumentKeyCommand]) as the window's shortcuts, listed on ? beside the window's Ctrl+/,
 * and F for full screen beside the window's F11. They take their keys before a focused control, so the arrows never
 * move focus, and leave them while [enabled] is false. The text size keys are the window's interface size keys, which
 * the reader takes while it is open; the search keys and they work while the search field has the cursor.
 *
 * Alt+← and the mouse's back button go back to where a link was followed from, ahead of the window's back, which goes on
 * to close what is open and leave the reader. Esc stays the window's back.
 */
@Composable
internal fun BookReaderKeys(controller: BookDocumentReaderController, fullScreen: WindowFullScreen?, enabled: Boolean) {
    val keys = controller.keys
    BookDocumentKeyCommand.entries.forEach { command ->
        // Every command is declared, those without keys in this reading mode off, so the list keeps their order
        val chords = command.chords(keys.scrolling)
        KeyboardShortcutHandler(shortcut(command, keys.scrolling, chords), enabled && chords.isNotEmpty()) {
            keys.run(command)
        }
    }
    KeyboardShortcutHandler(
        shortcut(
            "reader.returnFromLink",
            stringResource(MR.strings.reader_navigation_return),
            ShortcutKeys.Chord(Key.DirectionLeft, "←", alt = true),
            ShortcutKeys.MouseBack,
        ),
        enabled && controller.returnsFromJump,
        onTrigger = controller::returnToJumpOrigin,
    )
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
    KeyboardShortcutHandler(ScreenShortcuts.ShortcutList) { registry?.toggleList() }
}

@Composable
private fun shortcut(
    command: BookDocumentKeyCommand,
    scrolling: Boolean,
    chords: List<BookDocumentKeyChord>,
): KeyboardShortcut {
    val label = when (command) {
        BookDocumentKeyCommand.PageRight -> stringResource(MR.strings.reader_shortcut_page_right)
        BookDocumentKeyCommand.PageLeft -> stringResource(MR.strings.reader_shortcut_page_left)
        BookDocumentKeyCommand.NextPage -> stringResource(
            if (scrolling) MR.strings.book_reader_shortcut_screen_down else MR.strings.reader_shortcut_next_page,
        )
        BookDocumentKeyCommand.PreviousPage -> stringResource(
            if (scrolling) MR.strings.book_reader_shortcut_screen_up else MR.strings.reader_shortcut_previous_page,
        )
        BookDocumentKeyCommand.ScrollDown -> stringResource(MR.strings.book_reader_shortcut_scroll_down)
        BookDocumentKeyCommand.ScrollUp -> stringResource(MR.strings.book_reader_shortcut_scroll_up)
        BookDocumentKeyCommand.ChapterStart -> stringResource(MR.strings.book_reader_shortcut_chapter_start)
        BookDocumentKeyCommand.ChapterEnd -> stringResource(MR.strings.book_reader_shortcut_chapter_end)
        BookDocumentKeyCommand.NextChapter -> stringResource(MR.strings.action_next_chapter)
        BookDocumentKeyCommand.PreviousChapter -> stringResource(MR.strings.action_previous_chapter)
        BookDocumentKeyCommand.ToggleMenu -> stringResource(MR.strings.reader_shortcut_menu)
        BookDocumentKeyCommand.TextSizeUp -> stringResource(MR.strings.action_increase_text_size)
        BookDocumentKeyCommand.TextSizeDown -> stringResource(MR.strings.action_decrease_text_size)
        BookDocumentKeyCommand.TextSizeReset -> stringResource(MR.strings.book_reader_shortcut_text_reset)
        BookDocumentKeyCommand.SearchChapter -> stringResource(MR.strings.book_reader_search_hint)
        BookDocumentKeyCommand.SearchBook -> stringResource(MR.strings.book_reader_search_all_chapters)
        BookDocumentKeyCommand.NextMatch -> stringResource(MR.strings.action_next_match)
        BookDocumentKeyCommand.PreviousMatch -> stringResource(MR.strings.action_previous_match)
    }
    val keys = chords.map { chord ->
        ShortcutKeys.Chord(chord.key, keyName(chord), ctrl = chord.ctrl, shift = chord.shift)
    }
    return shortcut(
        "reader.${command.name}",
        label,
        *keys.toTypedArray(),
        whileTyping = chords.isNotEmpty() && chords.all { it.ctrl || it.key == Key.F3 },
    )
}

@Composable
private fun keyName(chord: BookDocumentKeyChord): String = when (chord.key) {
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
    Key.F3 -> "F3"
    Key.F -> "F"
    Key.N -> "N"
    Key.P -> "P"
    else -> error("No name for ${chord.key}")
}

private fun shortcut(id: String, label: String, vararg keys: ShortcutKeys, whileTyping: Boolean = false) =
    KeyboardShortcut(
        id = id,
        label = label,
        keys = keys.toList(),
        group = ShortcutGroup.CurrentScreen,
        whileTyping = whileTyping,
        beforeFocus = true,
    )
