package mihon.webview.jcef.surface

import androidx.compose.ui.input.key.Key
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LeftKeyTest {

    // Chromium reports Windows' key codes, which match AWT's for letters and function keys but not for Enter or the
    // symbol keys, so a page leaving Ctrl+= or Ctrl+/ would otherwise reach no shortcut, or another key's
    @Test
    fun `keys whose Windows code isn't AWT's reach the shortcuts as the keys pressed`() {
        LeftKey.of(windowsKeyCode = 0xBB, modifiers = CONTROL) shouldBe
            LeftKey(Key.Equals, ctrl = true, alt = false, shift = false, meta = false)
        LeftKey.of(windowsKeyCode = 0xBF, modifiers = CONTROL).key shouldBe Key.Slash
        LeftKey.of(windowsKeyCode = 0x0D, modifiers = 0).key shouldBe Key.Enter
        LeftKey.of(windowsKeyCode = 0x6B, modifiers = CONTROL or KEY_PAD).key shouldBe Key.NumPadAdd
        LeftKey.of(windowsKeyCode = 0x25, modifiers = ALT) shouldBe
            LeftKey(Key.DirectionLeft, ctrl = false, alt = true, shift = false, meta = false)
    }

    private companion object {
        // CEF's event flags; JCEF, which names them, comes with the Java runtime and isn't there for tests
        const val CONTROL = 1 shl 2
        const val ALT = 1 shl 3
        const val KEY_PAD = 1 shl 9
    }
}
