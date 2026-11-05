package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.compose.ui.platform.ComposeView
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.openReaderPageInWebView
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderThemedContent
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderViewer
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import eu.kanade.tachiyomi.ui.reader.viewer.current
import mihon.entry.interactions.runtime.EntryInteractionTheme

/**
 * The shared paged viewer in the activity's view tree, with the key events the activity hands to its viewer: volume
 * keys turn pages when the setting allows and the menu is closed, the arrows and page keys turn them, and Menu toggles
 * the menu. The wheel reaches the Compose content on its own.
 */
internal class AndroidPagedViewer private constructor(
    private val activity: ReaderActivity,
    private val shared: PagedViewer,
) : Viewer, ReaderViewer by shared {

    constructor(activity: ReaderActivity, direction: PagerDirection) : this(
        activity,
        PagedViewer(activity.controller, direction, openPageInWebView = activity::openReaderPageInWebView),
    )

    // Disposed when the activity drops the view for another viewer
    private val view = ComposeView(activity).apply {
        setContent {
            ReaderThemedContent(activity) {
                EntryInteractionTheme { shared.Content() }
            }
        }
    }

    override fun getView(): View = view

    override fun handleKeyEvent(event: KeyEvent): Boolean {
        val isUp = event.action == KeyEvent.ACTION_UP
        val ctrlPressed = event.metaState and KeyEvent.META_CTRL_ON != 0
        val settings = activity.viewModel.readerSettings
        val volumeKeysTurnPages = settings.volumeKeys.current && !activity.viewModel.state.value.menuVisible
        val inverted = settings.volumeKeysInverted.current
        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (!volumeKeysTurnPages) return false
                if (isUp) if (inverted) shared.moveUp() else shared.moveDown()
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (!volumeKeysTurnPages) return false
                if (isUp) if (inverted) shared.moveDown() else shared.moveUp()
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> if (isUp) if (ctrlPressed) shared.moveToNext() else shared.moveRight()
            KeyEvent.KEYCODE_DPAD_LEFT -> if (isUp) if (ctrlPressed) shared.moveToPrevious() else shared.moveLeft()
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_PAGE_DOWN -> if (isUp) shared.moveDown()
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_PAGE_UP -> if (isUp) shared.moveUp()
            KeyEvent.KEYCODE_MENU -> if (isUp) shared.toggleMenu()
            else -> return false
        }
        return true
    }

    override fun handleGenericMotionEvent(event: MotionEvent): Boolean = false
}
