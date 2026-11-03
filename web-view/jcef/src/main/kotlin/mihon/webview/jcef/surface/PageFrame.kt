package mihon.webview.jcef.surface

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.jetbrains.cef.SharedMemoryCache
import org.cef.browser.CefBrowser
import org.cef.callback.CefDragData
import org.cef.handler.CefNativeRenderHandler
import org.cef.handler.CefRenderHandlerAdapter
import org.cef.handler.CefScreenInfo
import org.cef.misc.CefRange
import java.awt.Cursor
import java.awt.Point
import java.awt.Rectangle
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.SwingUtilities
import kotlin.math.ceil

/**
 * Where an off-screen Chromium browser paints and what it asks of the screen it is shown on. Chromium lays the page out
 * in CSS pixels at the [ViewSize.scale] the surface gives it, the window's density, so a page is as large as the app's
 * own text and as sharp as the display, and paints the page and, while one is open, a popup such as a `<select>` list at
 * [popupBounds]. What it tells of the page's text caret goes to [caret], which a browser nobody types into has none of.
 * Chromium calls in on threads of its own; what the surface reads is Compose state, set on the main thread.
 */
internal class PageFrame(private val caret: PageCaret? = null) :
    CefRenderHandlerAdapter(),
    CefNativeRenderHandler {
    private val view = FramePixels()
    private val popup = FramePixels()
    private val sharedMemory = SharedMemoryCache()

    /** The page as Chromium last painted it, at its size in device pixels; null before the first paint. */
    var viewImage by mutableStateOf<ImageBitmap?>(null)
        private set

    /** The open popup as Chromium last painted it; null while none is open. */
    var popupImage by mutableStateOf<ImageBitmap?>(null)
        private set

    /** Where the open popup sits on the page, in CSS pixels; null while none is open. */
    var popupBounds by mutableStateOf<Rectangle?>(null)
        private set

    /** The pointer's shape over the page, one of [Cursor]'s predefined types, as JCEF hands Chromium's over. */
    var cursor by mutableStateOf(Cursor.DEFAULT_CURSOR)
        private set

    @Volatile
    var size = ViewSize(width = 1, height = 1, scale = 1.0)
        private set

    private val paintPending = AtomicBoolean()

    // Read and set on the main thread
    private var released = false

    /** Takes the surface's [widthPx] by [heightPx] at [scale] pixels per CSS pixel; what changed, if anything. */
    fun resize(widthPx: Int, heightPx: Int, scale: Double): ViewChange? {
        // Rounded up, so the page covers the surface; the pixel it may overhang is clipped
        val next = ViewSize(
            width = ceil(widthPx / scale).toInt().coerceAtLeast(1),
            height = ceil(heightPx / scale).toInt().coerceAtLeast(1),
            scale = scale,
        )
        val previous = size
        if (next == previous) return null
        size = next
        return if (next.scale != previous.scale) ViewChange.SCALE else ViewChange.SIZE
    }

    override fun getViewRect(browser: CefBrowser): Rectangle = Rectangle(0, 0, size.width, size.height)

    // The page is all the screen Chromium knows of, so it places its popups within the page, where they are drawn
    override fun getScreenInfo(browser: CefBrowser, screenInfo: CefScreenInfo): Boolean {
        val bounds = getViewRect(browser)
        screenInfo.Set(size.scale, COLOR_DEPTH, COLOR_DEPTH_PER_COMPONENT, false, bounds, bounds)
        return true
    }

    override fun getDeviceScaleFactor(browser: CefBrowser): Double = size.scale

    override fun getScreenPoint(browser: CefBrowser, viewPoint: Point): Point = Point(viewPoint)

    override fun onPopupShow(browser: CefBrowser, show: Boolean) {
        if (show) return
        popup.clear()
        onMainThread {
            popupBounds = null
            popupImage = null
            popup.releaseImage()
        }
    }

    override fun onPopupSize(browser: CefBrowser, size: Rectangle) {
        val bounds = Rectangle(size)
        onMainThread { popupBounds = bounds }
    }

    // Chromium's server paints into memory it shares with the app, under a lock the app takes too, and names the memory
    override fun onPaintWithSharedMem(
        browser: CefBrowser,
        popup: Boolean,
        dirtyRectsCount: Int,
        sharedMemName: String,
        sharedMemHandle: Long,
        width: Int,
        height: Int,
    ) {
        val memory = sharedMemory.get(sharedMemName, sharedMemHandle).apply {
            setWidth(width)
            setHeight(height)
            setDirtyRectsCount(dirtyRectsCount)
        }
        memory.lock()
        try {
            val dirty = memory.wrapRects().order(ByteOrder.nativeOrder()).asIntBuffer()
            val dirtyRects = Array(dirtyRectsCount) { Rectangle(dirty.get(), dirty.get(), dirty.get(), dirty.get()) }
            (if (popup) this.popup else view).write(memory.wrapRaster(), width, height, dirtyRects)
        } finally {
            memory.unlock()
        }
        // Chromium may paint again before the main thread takes the last paint; it then takes both at once
        if (paintPending.compareAndSet(false, true)) {
            onMainThread {
                paintPending.set(false)
                if (released) return@onMainThread
                viewImage = view.image()
                popupImage = popupBounds?.let { this.popup.image() }
            }
        }
    }

    /** Frees the pictures; on the main thread, as the page closes and no surface draws it any more. */
    fun release() {
        released = true
        viewImage = null
        popupImage = null
        view.releaseImage()
        popup.releaseImage()
    }

    // The shared memory is closed as JCEF lets go of it
    override fun disposeNativeResources() = Unit

    override fun onCursorChange(browser: CefBrowser, cursorType: Int): Boolean {
        onMainThread { cursor = cursorType }
        return true
    }

    override fun OnImeCompositionRangeChanged(browser: CefBrowser, range: CefRange, characterBounds: Array<Rectangle>) {
        caret?.onCompositionChanged(characterBounds)
    }

    override fun OnTextSelectionChanged(browser: CefBrowser, selectedText: String?, range: CefRange) {
        caret?.relocate()
    }

    // Dragging a link or an image out of the page would need the window's drag and drop; it is cancelled, which leaves
    // selecting text by dragging as it is
    override fun startDragging(browser: CefBrowser, dragData: CefDragData, mask: Int, x: Int, y: Int): Boolean = false

    private fun onMainThread(block: () -> Unit) = SwingUtilities.invokeLater(block)

    private companion object {
        const val COLOR_DEPTH = 32
        const val COLOR_DEPTH_PER_COMPONENT = 8
    }
}

/** The page's size in CSS pixels, and the device pixels per CSS pixel it is painted at. */
internal data class ViewSize(val width: Int, val height: Int, val scale: Double)

internal enum class ViewChange { SIZE, SCALE }
