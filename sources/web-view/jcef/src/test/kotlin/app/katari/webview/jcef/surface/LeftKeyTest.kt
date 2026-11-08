package app.katari.webview.jcef.surface

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.awt.event.KeyEvent

class LeftKeyTest {

    // Chromium reports Windows' key codes, which match AWT's for letters and function keys but not for Enter or the
    // symbol keys, so a page leaving Ctrl+= or Ctrl+/ would otherwise find no press to reach the shortcuts with, or
    // another key's
    @Test
    fun `keys whose Windows code isn't AWT's are found as the keys pressed`() {
        LeftKey.awtKeyCode(0xBB) shouldBe KeyEvent.VK_EQUALS
        LeftKey.awtKeyCode(0xBF) shouldBe KeyEvent.VK_SLASH
        LeftKey.awtKeyCode(0x0D) shouldBe KeyEvent.VK_ENTER
        LeftKey.awtKeyCode(0x6B) shouldBe KeyEvent.VK_ADD
        LeftKey.awtKeyCode(0x25) shouldBe KeyEvent.VK_LEFT
    }
}
