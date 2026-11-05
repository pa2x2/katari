package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.content.res.Configuration
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.openReaderPageInWebView
import eu.kanade.tachiyomi.ui.reader.readerBackgroundIsDark
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderViewer
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
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

/** [content] in the dark or light theme that suits the reader's background, which the reader theme picks. */
@Composable
private fun ReaderThemedContent(activity: ReaderActivity, content: @Composable () -> Unit) {
    val readerTheme = activity.viewModel.readerSettings.readerTheme.state.collectAsState().value.effectiveValue
    val context = LocalContext.current
    val base = LocalConfiguration.current
    val dark = context.readerBackgroundIsDark(readerTheme, activity.uiPreferences)
    val configuration = remember(base, dark) {
        Configuration(base).apply {
            val night = if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
        }
    }
    CompositionLocalProvider(LocalConfiguration provides configuration, content = content)
}
