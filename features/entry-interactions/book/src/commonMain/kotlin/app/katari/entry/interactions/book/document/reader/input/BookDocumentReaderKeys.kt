package app.katari.entry.interactions.book.document.reader.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import app.katari.presentation.kit.shortcut.LocalKeyboardShortcuts

/**
 * Where the reader's key commands run: the viewer shown moves through the chapter ([BookDocumentKeyCommand.moves]),
 * and the screen around it does the rest. Android hands it the keys its focused viewer gets
 * ([bookDocumentKeyCommands]); the desktop declares the commands as the window's shortcuts.
 */
@Stable
internal class BookDocumentReaderKeys {
    /** Whether the reader scrolls rather than turns pages, which gives some commands other keys or none. */
    var scrolling by mutableStateOf(false)
        internal set

    private var viewer: ((BookDocumentKeyCommand) -> Unit)? = null
    private var screen: ((BookDocumentKeyCommand) -> Unit)? = null

    fun run(command: BookDocumentKeyCommand) {
        (if (command.moves) viewer else screen)?.invoke(command)
    }

    /** Runs the commands that move through the chapter with [onMove] while this is composed. */
    @Composable
    fun ViewerMoves(onMove: (BookDocumentKeyCommand) -> Unit) {
        val current by rememberUpdatedState(onMove)
        DisposableEffect(this) {
            val handler: (BookDocumentKeyCommand) -> Unit = { current(it) }
            viewer = handler
            onDispose { if (viewer === handler) viewer = null }
        }
    }

    /** Runs the commands that act on the screen with [onCommand] while this is composed. */
    @Composable
    fun ScreenCommands(onCommand: (BookDocumentKeyCommand) -> Unit) {
        val current by rememberUpdatedState(onCommand)
        DisposableEffect(this) {
            val handler: (BookDocumentKeyCommand) -> Unit = { current(it) }
            screen = handler
            onDispose { if (screen === handler) screen = null }
        }
    }
}

/**
 * Hands the keys a focused viewer gets to [keys] while [enabled], with [extraCommand] for keys only it has, such as the
 * volume keys turning pages. A command runs as its key is let go, and the key is kept from the controls around it.
 * Where the window has keyboard shortcuts, as on the desktop, the reader's keys are declared there instead.
 */
@Composable
internal fun Modifier.bookDocumentKeyCommands(
    keys: BookDocumentReaderKeys,
    enabled: Boolean,
    extraCommand: (KeyEvent) -> BookDocumentKeyCommand? = { null },
): Modifier {
    if (LocalKeyboardShortcuts.current != null) return this
    return onPreviewKeyEvent { event ->
        if (!enabled) return@onPreviewKeyEvent false
        val command = bookDocumentKeyCommand(event, keys.scrolling) ?: extraCommand(event)
            ?: return@onPreviewKeyEvent false
        if (event.type == KeyEventType.KeyUp) keys.run(command)
        true
    }
}
