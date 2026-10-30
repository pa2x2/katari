package mihon.webview.jcef.surface

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.IntSize
import mihon.core.platform.clipboard.TextClipboard
import mihon.core.platform.link.LinkOpener
import mihon.presentation.kit.content.drawWebPageCanvas
import mihon.presentation.kit.feedback.PointerTooltip
import mihon.presentation.kit.shortcut.LocalKeyboardShortcuts
import java.awt.Cursor

/**
 * [page] drawn into the screen: the picture Chromium paints at the window's density over the web's white canvas, its
 * popups over it, and the kit's right-click menu and tooltips over both, so they and the app's own dialogs and sheets
 * draw over the page as over any screen. The page gets the mouse over it and, once clicked, the keyboard; it takes focus
 * as it is first shown, so the keys that scroll a page work at once.
 */
@Composable
internal fun ChromiumSurface(
    page: ChromiumPage,
    linkOpener: LinkOpener,
    clipboard: TextClipboard,
    modifier: Modifier,
) {
    val scale = LocalDensity.current.density.toDouble()
    val shortcuts = LocalKeyboardShortcuts.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val layout = remember { SurfaceLayout() }

    SideEffect {
        page.input.shortcuts = shortcuts
        page.input.onTakeFocus = { next ->
            focusManager.moveFocus(if (next) FocusDirection.Next else FocusDirection.Previous)
        }
        page.layout(layout.size.width, layout.size.height, scale)
    }
    DisposableEffect(page) {
        page.onShown()
        onDispose {
            page.input.contextMenu?.dismiss()
            page.input.contextMenu = null
            page.onHidden()
        }
    }
    LaunchedEffect(page) { focusRequester.requestFocus() }

    Box(
        modifier = modifier
            .onSizeChanged { size ->
                layout.size = size
                page.layout(size.width, size.height, scale)
            }
            .clipToBounds()
            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(page.frame.cursor)))
            .pageKeyInput(page)
            .focusRequester(focusRequester)
            .onFocusChanged { page.setFocus(it.isFocused) }
            .focusable()
            .pagePointerInput(
                page = page,
                onPress = focusRequester::requestFocus,
                onPointer = { layout.pointer = it },
            )
            .drawBehind { drawPage(page.frame) },
    ) {
        page.input.contextMenu?.let { PageContextMenu(page, it, linkOpener, clipboard) }
        val tooltip = page.input.tooltip
        // Where the pointer was as the page named the tooltip, which stays there while the pointer moves on
        val tooltipPosition = remember(tooltip) { layout.pointer }
        PointerTooltip(tooltip, tooltipPosition)
    }
}

private fun DrawScope.drawPage(frame: PageFrame) {
    val view = frame.viewImage ?: return
    drawWebPageCanvas()
    drawImage(view)
    val bounds = frame.popupBounds ?: return
    val popup = frame.popupImage ?: return
    val scale = frame.size.scale
    drawImage(popup, topLeft = Offset((bounds.x * scale).toFloat(), (bounds.y * scale).toFloat()))
}

/** What the surface knows of its own layout and the pointer over it, which nothing redraws on. */
private class SurfaceLayout {
    var size = IntSize.Zero
    var pointer = Offset.Zero
}
