package mihon.webview.jcef.surface

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import mihon.presentation.kit.shortcut.KeyboardShortcutRegistry
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefContextMenuParams
import org.cef.callback.CefMenuModel
import org.cef.callback.CefRunContextMenuCallback
import org.cef.handler.CefContextMenuHandlerAdapter
import org.cef.handler.CefFocusHandlerAdapter
import org.cef.handler.CefKeyboardHandler
import org.cef.handler.CefKeyboardHandlerAdapter
import java.awt.event.KeyEvent
import java.util.concurrent.ConcurrentHashMap
import javax.swing.SwingUtilities
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * What a page's surface and Chromium tell each other about the user's input beyond the events themselves: the
 * right-click menu Chromium asks for, the tooltip it names, the keys the page leaves to the window's shortcuts, focus
 * leaving the page by Tab, and whether the user has just acted on the page. The surface's side runs on the main thread,
 * Chromium's on threads of its own.
 */
internal class PageInput {
    /** The open right-click menu; null while none is. */
    var contextMenu by mutableStateOf<PageMenuRequest?>(null)

    /** The text the page shows as a tooltip under the pointer, such as an element's `title`; null while none. */
    var tooltip by mutableStateOf<String?>(null)
        private set

    /** The window's shortcuts, which get the keys the page leaves; null in a window without keyboard shortcuts. */
    @Volatile
    var shortcuts: KeyboardShortcutRegistry? = null

    /** Moves focus on from the page, forwards when [next], after the page's last element is tabbed past. */
    @Volatile
    var onTakeFocus: (next: Boolean) -> Unit = {}

    /**
     * Whether the last key pressed went to one of Chromium's popups, such as a `<select>`'s list or a date picker.
     * Chromium reports such a key as left by the page even when the popup used it, as Esc closing a date picker does,
     * and reports a key right after its press, so the last press stands for the key reported.
     */
    @Volatile
    private var pressedIntoPopup = false

    // The last press of each key the page was given, by AWT key code, for the modifiers it was pressed with
    private val presses = ConcurrentHashMap<Int, KeyEvent>()

    @Volatile
    private var lastUserAction: TimeMark? = null

    fun userActed() {
        lastUserAction = TimeSource.Monotonic.markNow()
    }

    /** The page is given [press], into one of Chromium's popups where [intoPopup]. */
    fun onKeyPressed(press: KeyEvent, intoPopup: Boolean) {
        userActed()
        pressedIntoPopup = intoPopup
        presses[press.keyCode] = press
    }

    /**
     * Whether a page asking for a new window now does it on the user's click or key, as Chromium tells pages through
     * their transient activation; JCEF doesn't pass on Chromium's own answer, so the same time limit stands in for it.
     */
    fun isUserGesture(): Boolean = lastUserAction?.let { it.elapsedNow() < USER_ACTIVATION_LIFETIME } ?: false

    fun showTooltip(text: String?) {
        tooltip = text?.takeIf { it.isNotBlank() }
    }

    val keyboardHandler: CefKeyboardHandler = object : CefKeyboardHandlerAdapter() {
        // Chromium tells of the keys the page didn't handle, which go to the window's shortcuts
        override fun onKeyEvent(browser: CefBrowser, event: CefKeyboardHandler.CefKeyEvent): Boolean {
            if (event.type != CefKeyboardHandler.CefKeyEvent.EventType.KEYEVENT_RAWKEYDOWN) return false
            if (pressedIntoPopup) return false
            val press = presses[LeftKey.awtKeyCode(event.windows_key_code)] ?: return false
            val key = LeftKey.of(press)
            // A key typed into the page's field, such as Ctrl+F, stays the field's as it does in the app's own fields
            val typing = event.focus_on_editable_field
            SwingUtilities.invokeLater {
                shortcuts?.onKeyDown(key.key, key.ctrl, key.alt, key.shift, key.meta, typing = typing)
            }
            return true
        }
    }

    val contextMenuHandler = object : CefContextMenuHandlerAdapter() {
        override fun runContextMenu(
            browser: CefBrowser,
            frame: CefFrame,
            params: CefContextMenuParams,
            model: CefMenuModel,
            callback: CefRunContextMenuCallback,
        ): Boolean {
            // The parameters only hold while Chromium waits for this call to return
            val request = PageMenuRequest.of(browser, params, callback)
            SwingUtilities.invokeLater {
                contextMenu?.dismiss()
                contextMenu = request
            }
            return true
        }
    }

    val focusHandler = object : CefFocusHandlerAdapter() {
        override fun onTakeFocus(browser: CefBrowser, next: Boolean) {
            SwingUtilities.invokeLater { onTakeFocus(next) }
        }
    }

    private companion object {
        // Chromium's transient activation lifetime
        val USER_ACTIVATION_LIFETIME = 5.seconds
    }
}
