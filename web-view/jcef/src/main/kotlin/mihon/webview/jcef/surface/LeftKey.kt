package mihon.webview.jcef.surface

import androidx.compose.ui.input.key.Key
import org.cef.misc.EventFlags
import java.awt.event.KeyEvent

/**
 * A key going down that the page left, with the modifiers held, as the window's shortcuts know keys.
 *
 * Chromium reports keys under Windows' virtual-key codes on every platform, and Compose's desktop keys are AWT's codes.
 * The two agree on letters, digits, F1 to F12, the numeric keypad, Escape and the keys that move the cursor, and
 * [AWT_KEY_CODES] holds the other keys where they differ.
 */
internal data class LeftKey(val key: Key, val ctrl: Boolean, val alt: Boolean, val shift: Boolean, val meta: Boolean) {
    companion object {
        /** The key Chromium reports under [windowsKeyCode], with its [EventFlags] [modifiers]. */
        fun of(windowsKeyCode: Int, modifiers: Int): LeftKey {
            fun has(flag: Int) = modifiers and flag != 0
            val location = if (has(EventFlags.EVENTFLAG_IS_KEY_PAD)) {
                KeyEvent.KEY_LOCATION_NUMPAD
            } else {
                KeyEvent.KEY_LOCATION_STANDARD
            }
            return LeftKey(
                key = Key(AWT_KEY_CODES[windowsKeyCode] ?: windowsKeyCode, location),
                ctrl = has(EventFlags.EVENTFLAG_CONTROL_DOWN),
                alt = has(EventFlags.EVENTFLAG_ALT_DOWN),
                shift = has(EventFlags.EVENTFLAG_SHIFT_DOWN),
                meta = has(EventFlags.EVENTFLAG_COMMAND_DOWN),
            )
        }
    }
}

// Windows' virtual-key code to AWT's, where they differ
private val AWT_KEY_CODES = buildMap {
    put(0x0D, KeyEvent.VK_ENTER)
    put(0x2C, KeyEvent.VK_PRINTSCREEN)
    put(0x2D, KeyEvent.VK_INSERT)
    put(0x2E, KeyEvent.VK_DELETE)
    put(0x5D, KeyEvent.VK_CONTEXT_MENU)
    for (index in 0 until 12) put(0x7C + index, KeyEvent.VK_F13 + index)
    put(0xBA, KeyEvent.VK_SEMICOLON)
    put(0xBB, KeyEvent.VK_EQUALS)
    put(0xBC, KeyEvent.VK_COMMA)
    put(0xBD, KeyEvent.VK_MINUS)
    put(0xBE, KeyEvent.VK_PERIOD)
    put(0xBF, KeyEvent.VK_SLASH)
    put(0xDB, KeyEvent.VK_OPEN_BRACKET)
    put(0xDC, KeyEvent.VK_BACK_SLASH)
    put(0xDD, KeyEvent.VK_CLOSE_BRACKET)
}
