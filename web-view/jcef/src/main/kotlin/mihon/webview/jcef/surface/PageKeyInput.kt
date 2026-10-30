package mihon.webview.jcef.surface

import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.input.key.onKeyEvent

/**
 * Hands every key the focused page gets to Chromium, typed characters included, as a browser gives its page every key
 * first. The keys the page leaves come back to the window's shortcuts through [PageInput.keyboardHandler], so Esc and
 * Alt+← go back and Ctrl+Q quits unless the page used them.
 */
internal fun Modifier.pageKeyInput(page: ChromiumPage): Modifier = onKeyEvent { event ->
    val awt = event.awtEventOrNull ?: return@onKeyEvent false
    page.send(awt)
    true
}
