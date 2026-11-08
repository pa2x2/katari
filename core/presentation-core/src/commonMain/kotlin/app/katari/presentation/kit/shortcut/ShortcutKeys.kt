package app.katari.presentation.kit.shortcut

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key

/** One way to trigger a [KeyboardShortcut]. */
sealed interface ShortcutKeys {
    /** [key] pressed with exactly the modifiers set here. [keyName] is the key's label as the list shows it. */
    data class Chord(
        val key: Key,
        val keyName: String,
        val ctrl: Boolean = false,
        val alt: Boolean = false,
        val shift: Boolean = false,
    ) : ShortcutKeys {
        internal fun matches(press: KeyPress): Boolean = press == this.press

        /** The press that triggers it, whatever the list names its key. */
        internal val press: KeyPress
            get() = KeyPress(key = key, ctrl = ctrl, alt = alt, shift = shift)
    }

    /** The mouse's back button. The window watches the pointer for it, so it is listed but never matched to a key. */
    data object MouseBack : ShortcutKeys
}

/** A key going down and the modifiers held with it. */
internal data class KeyPress(
    val key: Key,
    val ctrl: Boolean = false,
    val alt: Boolean = false,
    val shift: Boolean = false,
    val meta: Boolean = false,
)

internal fun KeyEvent.toKeyPress() = KeyPress(
    key = key,
    ctrl = isCtrlPressed,
    alt = isAltPressed,
    shift = isShiftPressed,
    meta = isMetaPressed,
)
