package mihon.webview.jcef.surface

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import mihon.core.common.web.ExtensionPage
import mihon.webview.jcef.page.extension.ExtensionBrowser
import mihon.webview.jcef.page.extension.ExtensionPageDisplay
import java.awt.Cursor

/**
 * An extension's page, which its own dialog shows, drawn into the app's dialog that stands for it: the picture
 * Chromium paints at the window's density over the web's white canvas, with the mouse over it and, once it has focus,
 * the keys going to the page. Esc is left to the dialog, as Android's back key is left to a dialog by its web view. The
 * page goes back to being hidden as this leaves the screen. [page] must be one of the app's Chromium extension pages.
 */
@Composable
fun ChromiumExtensionPage(page: ExtensionPage, modifier: Modifier = Modifier) {
    require(page is ExtensionBrowser) { "A page Chromium's extension pages didn't open" }
    ExtensionPageSurface(page.display, modifier)
}

@Composable
private fun ExtensionPageSurface(display: ExtensionPageDisplay, modifier: Modifier) {
    val scale = LocalDensity.current.density.toDouble()
    val focusRequester = remember { FocusRequester() }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var frame by remember { mutableStateOf<PageFrame?>(null) }

    DisposableEffect(display) {
        frame = display.show()
        onDispose {
            frame = null
            display.hide()
        }
    }
    SideEffect { display.layout(size.width, size.height, scale) }
    LaunchedEffect(display) { focusRequester.requestFocus() }

    val cursor = frame?.cursor ?: Cursor.DEFAULT_CURSOR
    Box(
        modifier = modifier
            .onSizeChanged { size = it }
            .clipToBounds()
            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(cursor)))
            .onKeyEvent { event ->
                val awt = event.awtEventOrNull ?: return@onKeyEvent false
                if (event.key == Key.Escape) return@onKeyEvent false
                display.send(awt)
                true
            }
            .focusRequester(focusRequester)
            .onFocusChanged { display.setFocus(it.isFocused) }
            .focusable()
            .pagePointerInput(
                page = display,
                onPress = {
                    focusRequester.requestFocus()
                    display.setFocus(true)
                },
                onPointer = {},
            )
            .drawBehind { frame?.let { drawPage(it) } },
    )
}
