package eu.kanade.tachiyomi.ui.browse.immersive

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import app.katari.presentation.kit.shortcut.KeyboardShortcut
import app.katari.presentation.kit.shortcut.KeyboardShortcutHandler
import app.katari.presentation.kit.shortcut.ShortcutGroup
import app.katari.presentation.kit.shortcut.ShortcutKeys
import eu.kanade.presentation.entry.entryTypePresentation
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The shown title's actions on keys, where the window has them, whether or not the controls show: Enter opens its
 * episode or chapter, unless a focused control takes Enter itself, D its details, and S adds it to the library or
 * removes it. Ctrl+/ lists them under the current screen.
 */
@Composable
internal fun ImmersiveEntryKeys(actions: ImmersiveEntryActions, enabled: Boolean) {
    val presentation = actions.entry.type.entryTypePresentation()
    val onOpenChapter = actions.onOpenChapter
    KeyboardShortcutHandler(
        shortcut = entryShortcut(
            id = "immersive.openChapter",
            label = stringResource(presentation.immersiveOpenLabel),
            key = ShortcutKeys.Chord(Key.Enter, "Enter"),
            beforeFocus = false,
        ),
        enabled = enabled && onOpenChapter != null,
    ) { onOpenChapter?.invoke() }
    KeyboardShortcutHandler(
        shortcut = entryShortcut(
            id = "immersive.details",
            label = stringResource(MR.strings.action_details),
            key = ShortcutKeys.Chord(Key.D, "D"),
        ),
        enabled = enabled,
    ) { actions.onDetails() }
    KeyboardShortcutHandler(
        shortcut = entryShortcut(
            id = "immersive.library",
            label = libraryActionLabel(actions.entry),
            key = ShortcutKeys.Chord(Key.S, "S"),
        ),
        enabled = enabled,
    ) { actions.onLibraryAction() }
}

private fun entryShortcut(id: String, label: String, key: ShortcutKeys, beforeFocus: Boolean = true) = KeyboardShortcut(
    id = id,
    label = label,
    keys = listOf(key),
    group = ShortcutGroup.CurrentScreen,
    whileTyping = false,
    beforeFocus = beforeFocus,
)
