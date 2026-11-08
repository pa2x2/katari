package app.katari.webview.jcef.surface

import androidx.compose.ui.input.key.Key
import java.awt.event.KeyEvent

/**
 * A key going down that the page left, as the window's shortcuts know keys: the key as it was pressed, with the
 * modifiers held then.
 *
 * Chromium names the key it reports by its Windows virtual-key code on every platform, and Compose's desktop keys are
 * AWT's codes. The two agree on letters, digits, F1 to F12, the numeric keypad, Escape and the keys that move the
 * cursor, and [AWT_KEY_CODES] holds the other keys where they differ. The modifiers and the keypad come from the press
 * itself, as JCEF's server reports the key event's type where its modifiers belong.
 */
internal data class LeftKey(val key: Key, val ctrl: Boolean, val alt: Boolean, val shift: Boolean, val meta: Boolean) {
    companion object {
        /** The AWT key code of the key Chromium reports under [windowsKeyCode]. */
        fun awtKeyCode(windowsKeyCode: Int): Int = AWT_KEY_CODES[windowsKeyCode] ?: windowsKeyCode

        fun of(press: KeyEvent) = LeftKey(
            key = Key(press.keyCode, press.keyLocation),
            ctrl = press.isControlDown,
            alt = press.isAltDown,
            shift = press.isShiftDown,
            meta = press.isMetaDown,
        )
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
