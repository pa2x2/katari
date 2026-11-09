package app.katari.entry.interactions.manga.reader.text.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import app.katari.presentation.kit.shortcut.KeyboardShortcut
import app.katari.presentation.kit.shortcut.ShortcutGroup
import app.katari.presentation.kit.shortcut.ShortcutKeys
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The reader's text keys, where the window has keyboard shortcuts: T turns text translation on and off, and while the
 * text toolbar offers them, S selects an area and O switches between the original text and its translations. Each is
 * declared beside the button that does the same, whose tooltip names it.
 */
internal object MangaReaderTextShortcuts {
    val Translate: KeyboardShortcut
        @Composable get() = shortcut(
            id = "reader.text.translate",
            label = stringResource(MR.strings.reader_text_translate),
            key = ShortcutKeys.Chord(Key.T, "T"),
        )

    val SelectArea: KeyboardShortcut
        @Composable get() = shortcut(
            id = "reader.text.selectArea",
            label = stringResource(MR.strings.reader_text_select_area),
            key = ShortcutKeys.Chord(Key.S, "S"),
        )

    /** Labelled as the button is: by what pressing it now shows. */
    @Composable
    fun ToggleOriginal(showOriginal: Boolean): KeyboardShortcut = shortcut(
        id = "reader.text.toggleOriginal",
        label = stringResource(
            if (showOriginal) MR.strings.reader_text_show_translations else MR.strings.reader_text_show_original,
        ),
        key = ShortcutKeys.Chord(Key.O, "O"),
    )

    private fun shortcut(id: String, label: String, key: ShortcutKeys) = KeyboardShortcut(
        id = id,
        label = label,
        keys = listOf(key),
        group = ShortcutGroup.CurrentScreen,
        whileTyping = false,
        beforeFocus = true,
    )
}
