package app.katari.webview.jcef.page

import org.cef.browser.CefBrowser
import org.cef.handler.CefNativeRenderHandler
import org.cef.handler.CefRenderHandlerAdapter
import org.cef.handler.CefScreenInfo
import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle

/**
 * The view of a browser nothing draws: a window's size for the page to lay out in and its scripts to read, a desktop
 * window's unless [size] is given another, and paints that are dropped. The page stays visible to Chromium, which
 * would otherwise slow its timers down.
 */
internal class UnpaintedView :
    CefRenderHandlerAdapter(),
    CefNativeRenderHandler {
    /** In CSS pixels. A browser that is open learns of a change through `wasResized`. */
    @Volatile
    var size: Dimension = Dimension(WIDTH, HEIGHT)

    override fun getViewRect(browser: CefBrowser): Rectangle = Rectangle(size)

    override fun getScreenInfo(browser: CefBrowser, screenInfo: CefScreenInfo): Boolean {
        val bounds = getViewRect(browser)
        screenInfo.Set(SCALE, COLOR_DEPTH, COLOR_DEPTH_PER_COMPONENT, false, bounds, bounds)
        return true
    }

    override fun getDeviceScaleFactor(browser: CefBrowser): Double = SCALE

    override fun getScreenPoint(browser: CefBrowser, viewPoint: Point): Point = Point(viewPoint)

    override fun onPaintWithSharedMem(
        browser: CefBrowser,
        popup: Boolean,
        dirtyRectsCount: Int,
        sharedMemName: String,
        sharedMemHandle: Long,
        width: Int,
        height: Int,
    ) = Unit

    override fun disposeNativeResources() = Unit

    private companion object {
        const val WIDTH = 1280
        const val HEIGHT = 800
        const val SCALE = 1.0
        const val COLOR_DEPTH = 32
        const val COLOR_DEPTH_PER_COMPONENT = 8
    }
}
