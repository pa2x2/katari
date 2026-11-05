package eu.kanade.tachiyomi.ui.reader.viewer.strip

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.compose.ui.platform.ComposeView
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPages
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderThemedContent
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderViewer
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import eu.kanade.tachiyomi.ui.reader.viewer.current
import mihon.entry.interactions.runtime.EntryInteractionTheme

/**
 * The shared long strip in the activity's view tree, with the volume keys the activity hands to its viewer, which
 * scroll when the setting allows and the menu is closed. The reader's other keys are the shared ones the activity runs,
 * and the wheel reaches the Compose content on its own.
 */
internal class AndroidStripViewer(
    private val activity: ReaderActivity,
    private val shared: StripViewer,
) : Viewer, ReaderViewer by shared {

    // Disposed when the activity drops the view for another viewer
    private val view = ComposeView(activity).apply {
        setContent {
            ReaderThemedContent(activity) {
                EntryInteractionTheme { ReaderPages(shared, activity.viewModel.readerSettings) }
            }
        }
    }

    override fun getView(): View = view

    override fun handleKeyEvent(event: KeyEvent): Boolean {
        val settings = activity.viewModel.readerSettings
        if (!settings.volumeKeys.current || activity.viewModel.state.value.menuVisible) return false
        val forward = when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> true
            KeyEvent.KEYCODE_VOLUME_UP -> false
            else -> return false
        }
        if (event.action == KeyEvent.ACTION_UP) {
            if (forward != settings.volumeKeysInverted.current) shared.scrollDown() else shared.scrollUp()
        }
        return true
    }

    override fun handleGenericMotionEvent(event: MotionEvent): Boolean = false
}
