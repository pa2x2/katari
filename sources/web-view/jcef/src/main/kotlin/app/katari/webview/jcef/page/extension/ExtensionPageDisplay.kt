package app.katari.webview.jcef.page.extension

import app.katari.webview.jcef.page.UnpaintedView
import app.katari.webview.jcef.surface.PageFrame
import app.katari.webview.jcef.surface.PagePointerTarget
import org.cef.browser.CefBrowser
import org.cef.callback.CefDragData
import org.cef.handler.CefNativeRenderHandler
import org.cef.handler.CefRenderHandlerAdapter
import org.cef.handler.CefScreenInfo
import org.cef.misc.CefRange
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent

/**
 * Where an extension page's browser paints and what it asks of the screen: while the page is hidden, nowhere, at a
 * desktop window's size ([UnpaintedView]); while a dialog of the app's shows it, the dialog's [frame], at the dialog's
 * size, with the dialog's mouse and keys going to the browser. The page keeps its document, scripts and bridges
 * either way, only being laid out again at its new size. Shown, hidden and given input on the main thread; Chromium
 * calls in on threads of its own.
 */
internal class ExtensionPageDisplay :
    CefRenderHandlerAdapter(),
    CefNativeRenderHandler,
    PagePointerTarget {
    private val hidden = UnpaintedView()

    /** The browser once [ExtensionBrowser] has opened it, until it closes. */
    @Volatile
    var browser: CefBrowser? = null

    /** What a showing dialog draws; null while the page is hidden. */
    @Volatile
    var frame: PageFrame? = null
        private set

    private val current: CefNativeRenderHandler
        get() = frame ?: hidden

    override val scale: Double
        get() = frame?.size?.scale ?: 1.0

    fun show(): PageFrame = PageFrame().also { frame = it }

    fun hide() {
        val shown = frame ?: return
        frame = null
        shown.release()
        val browser = browser ?: return
        browser.setFocus(false)
        browser.notifyScreenInfoChanged()
        val size = hidden.getViewRect(browser)
        browser.wasResized(size.width, size.height)
    }

    /** The showing dialog's page is [widthPx] by [heightPx] at [scale] pixels per CSS pixel. */
    fun layout(widthPx: Int, heightPx: Int, scale: Double) {
        val shown = frame ?: return
        shown.resize(widthPx, heightPx, scale) ?: return
        val browser = browser ?: return
        // The screen the page knows of is the dialog's, which it hadn't been while hidden
        browser.notifyScreenInfoChanged()
        browser.wasResized(shown.size.width, shown.size.height)
    }

    fun setFocus(focused: Boolean) {
        browser?.setFocus(focused)
    }

    override fun send(event: MouseEvent) {
        browser?.sendMouseEvent(event)
    }

    override fun send(event: MouseWheelEvent) {
        browser?.sendMouseWheelEvent(event)
    }

    fun send(event: KeyEvent) {
        browser?.sendKeyEvent(event)
    }

    override fun getViewRect(browser: CefBrowser): Rectangle = current.getViewRect(browser)

    override fun getScreenInfo(browser: CefBrowser, screenInfo: CefScreenInfo): Boolean =
        current.getScreenInfo(browser, screenInfo)

    override fun getDeviceScaleFactor(browser: CefBrowser): Double = current.getDeviceScaleFactor(browser)

    override fun getScreenPoint(browser: CefBrowser, viewPoint: Point): Point =
        current.getScreenPoint(browser, viewPoint)

    override fun onPopupShow(browser: CefBrowser, show: Boolean) = current.onPopupShow(browser, show)

    override fun onPopupSize(browser: CefBrowser, size: Rectangle) = current.onPopupSize(browser, size)

    override fun onPaintWithSharedMem(
        browser: CefBrowser,
        popup: Boolean,
        dirtyRectsCount: Int,
        sharedMemName: String,
        sharedMemHandle: Long,
        width: Int,
        height: Int,
    ) = current.onPaintWithSharedMem(browser, popup, dirtyRectsCount, sharedMemName, sharedMemHandle, width, height)

    override fun onCursorChange(browser: CefBrowser, cursorType: Int): Boolean =
        current.onCursorChange(browser, cursorType)

    override fun startDragging(browser: CefBrowser, dragData: CefDragData, mask: Int, x: Int, y: Int): Boolean =
        current.startDragging(browser, dragData, mask, x, y)

    override fun OnImeCompositionRangeChanged(browser: CefBrowser, range: CefRange, characterBounds: Array<Rectangle>) =
        current.OnImeCompositionRangeChanged(browser, range, characterBounds)

    override fun OnTextSelectionChanged(browser: CefBrowser, selectedText: String?, range: CefRange) =
        current.OnTextSelectionChanged(browser, selectedText, range)

    // Each frame frees its own pictures as its dialog closes, and the hidden view holds none
    override fun disposeNativeResources() = Unit
}
