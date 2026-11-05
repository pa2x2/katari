package eu.kanade.tachiyomi.ui.reader.viewer.strip

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
 * The shared long strip in the activity's view tree, with the key events the activity hands to its viewer: volume keys
 * scroll when the setting allows and the menu is closed, the arrows and page keys scroll, and Menu toggles the menu.
 * The wheel reaches the Compose content on its own.
 */
internal class AndroidStripViewer private constructor(
    private val activity: ReaderActivity,
    private val shared: StripViewer,
) : Viewer, ReaderViewer by shared {

    constructor(activity: ReaderActivity, continuous: Boolean) : this(
        activity,
        StripViewer(activity.controller, continuous, openPageInWebView = activity::openReaderPageInWebView),
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
        val settings = activity.viewModel.readerSettings
        val volumeKeysScroll = settings.volumeKeys.current && !activity.viewModel.state.value.menuVisible
        val inverted = settings.volumeKeysInverted.current
        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (!volumeKeysScroll) return false
                if (isUp) if (inverted) shared.scrollUp() else shared.scrollDown()
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (!volumeKeysScroll) return false
                if (isUp) if (inverted) shared.scrollDown() else shared.scrollUp()
            }
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_PAGE_UP ->
                if (isUp) shared.scrollUp()
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_PAGE_DOWN ->
                if (isUp) shared.scrollDown()
            KeyEvent.KEYCODE_MENU -> if (isUp) shared.toggleMenu()
            else -> return false
        }
        return true
    }

    override fun handleGenericMotionEvent(event: MotionEvent): Boolean = false
}
