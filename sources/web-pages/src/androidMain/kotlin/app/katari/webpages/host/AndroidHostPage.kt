package app.katari.webpages.host

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewCompat
import eu.kanade.tachiyomi.source.entry.webpage.WebPageException
import eu.kanade.tachiyomi.source.entry.webpage.WebPageKey
import eu.kanade.tachiyomi.util.system.setDefaultSettings
import eu.kanade.tachiyomi.util.system.setUserAgent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * One page in a web view no window holds. Such a view has no size of its own, and a page in it sees a window of
 * nothing, so it is laid out by hand at the page's size. It takes touches, text and keys as an attached one does, and
 * the page takes them for a person's.
 *
 * Nothing draws such a view either, and a page that is never drawn never starts a transition or an animation: what a
 * site slides or fades in stays where it began, and `transitionend` never comes. So while the page isn't idle the
 * view is drawn here, shrunk into a bitmap nobody looks at, which is all its animations need to run.
 *
 * Made and called on the main thread; every call here moves there.
 */
internal class AndroidHostPage(
    context: Context,
    settings: HostPageSettings,
    userAgent: String,
    private val events: HostPageEvents,
) : HostPage {
    private val density = context.resources.displayMetrics.density
    private val view = WebView(context)

    private var load: CompletableDeferred<Unit>? = null

    // After the renderer ended or the view was destroyed, nothing but destroying it may be called on it
    private var isOver = false
    private var fingerDownAt = 0L

    // A view no window holds runs nothing posted to it, so the main thread's own queue paces the frames
    private val frames = Handler(Looper.getMainLooper())
    private val frame = Canvas()
    private val drawFrame = object : Runnable {
        override fun run() {
            if (isOver) return
            view.draw(frame)
            frames.postDelayed(this, FRAME_EVERY.inWholeMilliseconds)
        }
    }

    init {
        view.setDefaultSettings()
        view.setUserAgent(userAgent)
        val width = (settings.width * density).roundToInt()
        val height = (settings.height * density).roundToInt()
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, width, height)
        // The whole view, so that all of the page counts as drawn, at a size that costs next to nothing to paint
        frame.setBitmap(Bitmap.createBitmap(width / FRAME_SHRUNK, height / FRAME_SHRUNK, Bitmap.Config.ARGB_8888))
        frame.scale(1f / FRAME_SHRUNK, 1f / FRAME_SHRUNK)
        WebViewCompat.addWebMessageListener(view, settings.channel, EVERY_SITE) { _, message, _, isMainFrame, _ ->
            if (isMainFrame) message.data?.let(events::onMessage)
        }
        WebViewCompat.addDocumentStartJavaScript(view, settings.startScript, EVERY_SITE)
        view.webViewClient = Client()
        frames.post(drawFrame)
    }

    override suspend fun load(url: String, headers: Map<String, String>) = onMain {
        val finished = CompletableDeferred<Unit>()
        load = finished
        view.loadUrl(url, headers)
        finished.await()
    }

    override suspend fun evaluate(script: String): String = onMain {
        suspendCancellableCoroutine { continuation ->
            view.evaluateJavascript(script) { value -> continuation.resume(value ?: NO_VALUE) }
        }
    }

    override suspend fun touch(step: HostTouch, x: Float, y: Float) = onMain {
        val now = SystemClock.uptimeMillis()
        if (step == HostTouch.DOWN) fingerDownAt = now
        val action = when (step) {
            HostTouch.DOWN -> MotionEvent.ACTION_DOWN
            HostTouch.MOVE -> MotionEvent.ACTION_MOVE
            HostTouch.UP -> MotionEvent.ACTION_UP
        }
        val event = MotionEvent.obtain(fingerDownAt, now, action, x * density, y * density, 0)
        view.dispatchTouchEvent(event)
        event.recycle()
    }

    override suspend fun insertText(text: String) = onMain {
        // The view learns of the focused field from its renderer a moment after a script focused it
        val connection = withTimeoutOrNull(FIELD_TIMEOUT) {
            var connection: InputConnection? = view.onCreateInputConnection(EditorInfo())
            while (connection == null) {
                delay(FIELD_ASKED_AGAIN)
                connection = view.onCreateInputConnection(EditorInfo())
            }
            connection
        } ?: throw WebPageException("Nothing that takes text has the page's focus")
        connection.commitText(text, CURSOR_AFTER_TEXT)
        Unit
    }

    override suspend fun press(key: WebPageKey) = onMain {
        val pressed = SystemClock.uptimeMillis()
        val code = key.keyCode()
        view.dispatchKeyEvent(KeyEvent(pressed, pressed, KeyEvent.ACTION_DOWN, code, 0))
        view.dispatchKeyEvent(KeyEvent(pressed, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, code, 0))
        Unit
    }

    // A paused view's page counts as hidden: its timers slow to one a second, and it is drawn no more
    override suspend fun setIdle(idle: Boolean) = onMain {
        frames.removeCallbacks(drawFrame)
        if (idle) {
            view.onPause()
        } else {
            view.onResume()
            frames.post(drawFrame)
        }
        Unit
    }

    override suspend fun close() = withContext(Dispatchers.Main.immediate) {
        if (!isOver) view.stopLoading()
        end()
    }

    private suspend fun <T> onMain(block: suspend () -> T): T = withContext(Dispatchers.Main.immediate) {
        if (isOver) throw WebPageException(PAGE_OVER)
        block()
    }

    private fun end() {
        if (isOver) return
        isOver = true
        frames.removeCallbacks(drawFrame)
        load?.completeExceptionally(WebPageException(PAGE_OVER))
        view.destroy()
    }

    private fun WebPageKey.keyCode(): Int = when (this) {
        WebPageKey.ENTER -> KeyEvent.KEYCODE_ENTER
        WebPageKey.TAB -> KeyEvent.KEYCODE_TAB
        WebPageKey.ESCAPE -> KeyEvent.KEYCODE_ESCAPE
        WebPageKey.BACKSPACE -> KeyEvent.KEYCODE_DEL
        WebPageKey.ARROW_UP -> KeyEvent.KEYCODE_DPAD_UP
        WebPageKey.ARROW_DOWN -> KeyEvent.KEYCODE_DPAD_DOWN
        WebPageKey.ARROW_LEFT -> KeyEvent.KEYCODE_DPAD_LEFT
        WebPageKey.ARROW_RIGHT -> KeyEvent.KEYCODE_DPAD_RIGHT
    }

    private inner class Client : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            load?.complete(Unit)
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (!request.isForMainFrame) return
            load?.completeExceptionally(WebPageException("${request.url} couldn't be loaded: ${error.description}"))
        }

        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            // Every web view of the app shares the renderer; leaving its end unhandled ends the app
            logcat(LogPriority.ERROR) { "A source's page lost its renderer: crashed=${detail.didCrash()}" }
            end()
            events.onGone()
            return true
        }
    }

    private companion object {
        val EVERY_SITE = setOf("*")
        val FIELD_TIMEOUT = 2.seconds
        val FRAME_EVERY = 33.milliseconds
        const val FRAME_SHRUNK = 8
        val FIELD_ASKED_AGAIN = 50.milliseconds
        const val CURSOR_AFTER_TEXT = 1
        const val NO_VALUE = "null"
        const val PAGE_OVER = "The page was closed"
    }
}
