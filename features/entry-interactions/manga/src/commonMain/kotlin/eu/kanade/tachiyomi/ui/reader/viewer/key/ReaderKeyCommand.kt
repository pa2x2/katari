package eu.kanade.tachiyomi.ui.reader.viewer.key

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import mihon.presentation.kit.shortcut.ContextMenuKey

/**
 * What the reader's keys do, on every platform. The arrows turn toward their side of the screen, so in right to left
 * ← is the next page; with Ctrl they, like ↓ and ↑, go in reading order. The long strip scrolls a screen for each of
 * the page commands. The zoom keys zoom the page shown, or the strip, about the middle of the screen.
 */
internal enum class ReaderKeyCommand {
    PageRight,
    PageLeft,
    NextPage,
    PreviousPage,
    FirstPage,
    LastPage,
    NextChapter,
    PreviousChapter,
    ToggleMenu,
    ZoomIn,
    ZoomOut,
    ZoomFit,
}

/** A key, pressed with exactly the modifiers set here, that runs a [ReaderKeyCommand]. */
internal data class ReaderKeyChord(val key: Key, val ctrl: Boolean = false, val shift: Boolean = false)

internal val ReaderKeyCommand.chords: List<ReaderKeyChord>
    get() = when (this) {
        ReaderKeyCommand.PageRight -> listOf(ReaderKeyChord(Key.DirectionRight))
        ReaderKeyCommand.PageLeft -> listOf(ReaderKeyChord(Key.DirectionLeft))
        ReaderKeyCommand.NextPage -> listOf(
            ReaderKeyChord(Key.DirectionRight, ctrl = true),
            ReaderKeyChord(Key.DirectionDown),
            ReaderKeyChord(Key.PageDown),
        )
        ReaderKeyCommand.PreviousPage -> listOf(
            ReaderKeyChord(Key.DirectionLeft, ctrl = true),
            ReaderKeyChord(Key.DirectionUp),
            ReaderKeyChord(Key.PageUp),
        )
        ReaderKeyCommand.FirstPage -> listOf(ReaderKeyChord(Key.MoveHome))
        ReaderKeyCommand.LastPage -> listOf(ReaderKeyChord(Key.MoveEnd))
        ReaderKeyCommand.NextChapter -> listOf(ReaderKeyChord(Key.N))
        ReaderKeyCommand.PreviousChapter -> listOf(ReaderKeyChord(Key.P))
        ReaderKeyCommand.ToggleMenu -> listOf(ReaderKeyChord(ContextMenuKey))
        ReaderKeyCommand.ZoomIn -> listOf(
            ReaderKeyChord(Key.Equals, ctrl = true),
            // Where + is Shift and =
            ReaderKeyChord(Key.Equals, ctrl = true, shift = true),
            ReaderKeyChord(Key.NumPadAdd, ctrl = true),
        )
        ReaderKeyCommand.ZoomOut -> listOf(
            ReaderKeyChord(Key.Minus, ctrl = true),
            ReaderKeyChord(Key.NumPadSubtract, ctrl = true),
        )
        ReaderKeyCommand.ZoomFit -> listOf(
            ReaderKeyChord(Key.Zero, ctrl = true),
            ReaderKeyChord(Key.NumPad0, ctrl = true),
        )
    }

/** The command [event]'s key and modifiers run, or null when they run none. */
internal fun readerKeyCommand(event: KeyEvent): ReaderKeyCommand? {
    if (event.isAltPressed) return null
    val chord = ReaderKeyChord(event.key, ctrl = event.isCtrlPressed, shift = event.isShiftPressed)
    return ReaderKeyCommand.entries.firstOrNull { chord in it.chords }
}
