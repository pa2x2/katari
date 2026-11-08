package app.katari.entry.interactions.player

import androidx.compose.runtime.Composable

/**
 * The player's picture-in-picture window as the desktop app opens it: a small window without a title bar, which stays
 * over the others where the desktop lets a window ask to. It opens where and as large as it last was, the first time
 * in the bottom-right corner a quarter of the screen wide, and keeps the video's shape as it is resized from its
 * edges. Dragging it anywhere [content] leaves the pointer to moves it, and a double-click there calls
 * [onDoubleClick]. The window has keyboard shortcuts of its own, which the shortcuts declared in [content] answer
 * while it has focus.
 */
interface AnimePlayerPictureInPictureWindow {
    /**
     * Shows the window while this is composed. [aspectRatio] is the video's, null until it has one; [title] names the
     * window where the desktop lists windows. [onCloseRequest] when the desktop closes it, as from its window list.
     */
    @Composable
    fun Window(
        title: String,
        aspectRatio: Float?,
        onDoubleClick: () -> Unit,
        onCloseRequest: () -> Unit,
        content: @Composable () -> Unit,
    )
}
