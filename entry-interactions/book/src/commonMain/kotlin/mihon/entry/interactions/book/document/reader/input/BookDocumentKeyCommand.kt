package mihon.entry.interactions.book.document.reader.input

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import mihon.presentation.kit.shortcut.ContextMenuKey

/**
 * What the book reader's keys do, on every platform. Turning pages, the arrows go toward their side of the screen, so
 * in right to left ← is the next page; with Ctrl they, like ↓, ↑, Page Down and Page Up, go in reading order.
 * Scrolling, ↓ and ↑ move a few lines and Page Down and Page Up a screen, while ← and → do nothing. Home and End go to
 * the start and end of the chapter either way, and N and P to the next and previous: a chapter here is a section of
 * the book, as the position bar counts and steps them.
 */
internal enum class BookDocumentKeyCommand {
    PageRight,
    PageLeft,
    NextPage,
    PreviousPage,
    ScrollDown,
    ScrollUp,
    ChapterStart,
    ChapterEnd,
    NextChapter,
    PreviousChapter,
    ToggleMenu,
    TextSizeUp,
    TextSizeDown,
    TextSizeReset,
    SearchChapter,
    SearchBook,
    NextMatch,
    PreviousMatch,
    ;

    /** Whether it moves through the chapter, which the viewer shown does, rather than the screen around it. */
    val moves: Boolean
        get() = this in MOVES

    private companion object {
        val MOVES = setOf(PageRight, PageLeft, NextPage, PreviousPage, ScrollDown, ScrollUp, ChapterStart, ChapterEnd)
    }
}

/** A key, pressed with exactly the modifiers set here, that runs a [BookDocumentKeyCommand]. */
internal data class BookDocumentKeyChord(val key: Key, val ctrl: Boolean = false, val shift: Boolean = false)

/** The keys of this command while the reader [scrolls][scrolling] or turns pages; none where it does nothing. */
internal fun BookDocumentKeyCommand.chords(scrolling: Boolean): List<BookDocumentKeyChord> = when (this) {
    BookDocumentKeyCommand.PageRight -> if (scrolling) emptyList() else listOf(BookDocumentKeyChord(Key.DirectionRight))
    BookDocumentKeyCommand.PageLeft -> if (scrolling) emptyList() else listOf(BookDocumentKeyChord(Key.DirectionLeft))
    BookDocumentKeyCommand.NextPage -> if (scrolling) {
        listOf(BookDocumentKeyChord(Key.PageDown))
    } else {
        listOf(
            BookDocumentKeyChord(Key.DirectionRight, ctrl = true),
            BookDocumentKeyChord(Key.DirectionDown),
            BookDocumentKeyChord(Key.PageDown),
        )
    }
    BookDocumentKeyCommand.PreviousPage -> if (scrolling) {
        listOf(BookDocumentKeyChord(Key.PageUp))
    } else {
        listOf(
            BookDocumentKeyChord(Key.DirectionLeft, ctrl = true),
            BookDocumentKeyChord(Key.DirectionUp),
            BookDocumentKeyChord(Key.PageUp),
        )
    }
    BookDocumentKeyCommand.ScrollDown -> if (scrolling) listOf(BookDocumentKeyChord(Key.DirectionDown)) else emptyList()
    BookDocumentKeyCommand.ScrollUp -> if (scrolling) listOf(BookDocumentKeyChord(Key.DirectionUp)) else emptyList()
    BookDocumentKeyCommand.ChapterStart -> listOf(BookDocumentKeyChord(Key.MoveHome))
    BookDocumentKeyCommand.ChapterEnd -> listOf(BookDocumentKeyChord(Key.MoveEnd))
    BookDocumentKeyCommand.NextChapter -> listOf(BookDocumentKeyChord(Key.N))
    BookDocumentKeyCommand.PreviousChapter -> listOf(BookDocumentKeyChord(Key.P))
    BookDocumentKeyCommand.ToggleMenu -> listOf(BookDocumentKeyChord(ContextMenuKey))
    BookDocumentKeyCommand.TextSizeUp -> listOf(
        BookDocumentKeyChord(Key.Equals, ctrl = true),
        // Where + is Shift and =
        BookDocumentKeyChord(Key.Equals, ctrl = true, shift = true),
        BookDocumentKeyChord(Key.NumPadAdd, ctrl = true),
    )
    BookDocumentKeyCommand.TextSizeDown -> listOf(
        BookDocumentKeyChord(Key.Minus, ctrl = true),
        BookDocumentKeyChord(Key.NumPadSubtract, ctrl = true),
    )
    BookDocumentKeyCommand.TextSizeReset -> listOf(
        BookDocumentKeyChord(Key.Zero, ctrl = true),
        BookDocumentKeyChord(Key.NumPad0, ctrl = true),
    )
    BookDocumentKeyCommand.SearchChapter -> listOf(BookDocumentKeyChord(Key.F, ctrl = true))
    BookDocumentKeyCommand.SearchBook -> listOf(BookDocumentKeyChord(Key.F, ctrl = true, shift = true))
    BookDocumentKeyCommand.NextMatch -> listOf(BookDocumentKeyChord(Key.F3))
    BookDocumentKeyCommand.PreviousMatch -> listOf(BookDocumentKeyChord(Key.F3, shift = true))
}

/** The command [event]'s key and modifiers run while the reader [scrolls][scrolling], or null when they run none. */
internal fun bookDocumentKeyCommand(event: KeyEvent, scrolling: Boolean): BookDocumentKeyCommand? {
    if (event.isAltPressed) return null
    val chord = BookDocumentKeyChord(event.key, ctrl = event.isCtrlPressed, shift = event.isShiftPressed)
    return BookDocumentKeyCommand.entries.firstOrNull { chord in it.chords(scrolling) }
}
