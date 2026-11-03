package mihon.webview.jcef.page.extension

import mihon.core.common.web.ExtensionPageTouch
import mihon.core.common.web.ExtensionPageTouch.Phase
import mihon.core.common.web.ExtensionPageTouch.Tool
import org.cef.input.CefTouchEvent

/**
 * The touch as Chromium takes it from a windowless browser, in its view's coordinates, which are the page's CSS pixels
 * as hidden pages lay out at a scale of one. Chromium stamps the time it arrives, and no keys are held.
 */
internal fun ExtensionPageTouch.toCefTouchEvent() = CefTouchEvent(
    pointerId,
    x,
    y,
    radiusX,
    radiusY,
    rotation,
    pressure,
    when (phase) {
        Phase.PRESSED -> CefTouchEvent.EventType.PRESSED
        Phase.MOVED -> CefTouchEvent.EventType.MOVED
        Phase.RELEASED -> CefTouchEvent.EventType.RELEASED
        Phase.CANCELLED -> CefTouchEvent.EventType.CANCELLED
    },
    NO_MODIFIERS,
    when (tool) {
        Tool.FINGER -> CefTouchEvent.PointerType.TOUCH
        Tool.STYLUS -> CefTouchEvent.PointerType.PEN
        Tool.ERASER -> CefTouchEvent.PointerType.ERASER
        Tool.MOUSE -> CefTouchEvent.PointerType.MOUSE
    },
)

private const val NO_MODIFIERS = 0
