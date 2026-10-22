package app.katari.showcase.capture

import android.content.Intent
import android.graphics.Rect
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.SearchCondition
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.io.File
import kotlin.math.roundToInt

/**
 * The app under capture and the device it runs on. Scenes drive it through the UI only, record stills to
 * [CAPTURE_DIRECTORY] with the shell's screencap, and the pipeline pulls that directory afterwards. The pipeline passes
 * the app's package as the `targetAppId` instrumentation argument.
 */
internal object ShowcaseDevice {
    const val CAPTURE_DIRECTORY = "/data/local/tmp/showcase"
    const val FAILURE_DIRECTORY = "/data/local/tmp/showcase-failures"

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    val device: UiDevice by lazy { UiDevice.getInstance(instrumentation) }

    val appId: String by lazy { argument("targetAppId") }

    fun argument(name: String): String =
        InstrumentationRegistry.getArguments().getString(name) ?: error("Instrumentation argument $name is missing")

    fun shell(command: String): String = device.executeShellCommand(command)

    /** Writes [text] to [path] on the device. Shell commands take no redirection, so it goes through this app's cache. */
    fun writeDeviceFile(path: String, text: String) {
        val file = File(instrumentation.context.cacheDir, File(path).name)
        file.writeText(text)
        shell("cp ${file.path} $path")
        file.delete()
    }

    fun launch() {
        val context = instrumentation.context
        val intent = context.packageManager.getLaunchIntentForPackage(appId) ?: error("$appId has no launcher activity")
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        device.wait(Until.hasObject(By.pkg(appId).depth(0)), TIMEOUT_MS)
    }

    fun restart() {
        shell("am force-stop $appId")
        launch()
    }

    fun find(selector: BySelector, timeoutMs: Long = TIMEOUT_MS): UiObject2 =
        device.wait(Until.findObject(selector), timeoutMs) ?: fail("Nothing on screen matches $selector")

    /**
     * Scrolls the screen down until [selector] is on screen, by dragging through the middle of the screen as a reader
     * would. Whatever scrollable comes first in the tree may be a row of results that scrolls sideways, or the page may
     * not scroll yet while it loads, and a drag in the middle never starts where the system takes swipes as its own.
     */
    fun scrollTo(selector: BySelector, timeoutMs: Long = TIMEOUT_MS) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        val x = device.displayWidth / 2
        while (SystemClock.uptimeMillis() < deadline) {
            if (device.hasObject(selector)) return
            device.swipe(
                x,
                (device.displayHeight * SCROLL_FROM).toInt(),
                x,
                (device.displayHeight * SCROLL_TO).toInt(),
                SCROLL_STEPS,
            )
            settle(extraMs = 300)
        }
        fail("Scrolling never reached $selector")
    }

    /**
     * The scrollable matching [selector], with its gestures kept out of the bands along the top and bottom edges where
     * the system takes a swipe as its own: one starting at the bottom edge opens recents in place of scrolling.
     * Elsewhere they keep UiAutomator's default margin of a tenth of the size, so scrolls go as far as they always did.
     */
    fun list(selector: BySelector = By.scrollable(true)): UiObject2 {
        val list = find(selector)
        val bounds = list.visibleBounds
        val density = instrumentation.context.resources.displayMetrics.density
        val safeTop = (TOP_SYSTEM_GESTURES_DP * density).toInt()
        val safeBottom = device.displayHeight - (BOTTOM_SYSTEM_GESTURES_DP * density).toInt()
        val horizontal = (bounds.width() * DEFAULT_GESTURE_MARGIN).toInt()
        val vertical = (bounds.height() * DEFAULT_GESTURE_MARGIN).toInt()
        val top = maxOf(vertical, safeTop - bounds.top)
        val bottom = maxOf(vertical, bounds.bottom - safeBottom)
        if (bounds.height() - top - bottom < MIN_GESTURE_HEIGHT_PX) fail("$selector has no room for a gesture")
        list.setGestureMargins(horizontal, top, horizontal, bottom)
        return list
    }

    /**
     * Scrolls the screen down by dragging [fraction] of its height up from where [scrollTo] starts, holding still before
     * letting go. A list let go of while it moves glides on, however slowly it was dragged, and how far depends on the
     * timing of the last touch events, so a still taken partway down would shift between runs. The drag is the same
     * whole pixels every run, read from the display rather than from a list that may still be settling.
     */
    fun dragUp(fraction: Float) = dragUpBy((device.displayHeight * fraction).roundToInt())

    private fun dragUpBy(distancePx: Int) {
        val x = (device.displayWidth / 2).toFloat()
        val startY = (device.displayHeight * SCROLL_FROM).roundToInt().toFloat()
        val endY = startY - distancePx
        val downTime = SystemClock.uptimeMillis()
        touch(MotionEvent.ACTION_DOWN, downTime, x, startY)
        var y = startY
        while (y > endY) {
            SystemClock.sleep(DRAG_STEP_MS)
            y = maxOf(endY, y - DRAG_STEP_PX)
            touch(MotionEvent.ACTION_MOVE, downTime, x, y)
        }
        // Velocity trackers take a finger that rested this long before lifting as having no speed left.
        repeat(DRAG_HOLD_MOVES) {
            SystemClock.sleep(DRAG_HOLD_INTERVAL_MS)
            touch(MotionEvent.ACTION_MOVE, downTime, x, endY)
        }
        touch(MotionEvent.ACTION_UP, downTime, x, endY)
    }

    fun tap(selector: BySelector, timeoutMs: Long = TIMEOUT_MS) {
        try {
            tap(findOrUncover(selector, timeoutMs))
        } catch (_: StaleObjectException) {
            // Redrawn between finding and tapping it, as controls that follow playback and screens still sliding in
            // are.
            tap(findOrUncover(selector, timeoutMs))
        }
    }

    /**
     * Finds [selector], or failing that scrolls the screen up from under the home dock and looks again. Compose leaves
     * a view that the dock covers whole out of the tree, so one that a UI change moved under the dock would never be
     * found. Scrolling only once the wait is over keeps a screen that is merely slow to show it as it was.
     */
    private fun findOrUncover(selector: BySelector, timeoutMs: Long): UiObject2 {
        device.wait(Until.findObject(selector), timeoutMs)?.let { return it }
        repeat(REVEAL_ATTEMPTS) {
            val dock =
                device.findObject(By.res(FLOATING_DOCK))?.visibleBounds ?: fail("Nothing on screen matches $selector")
            dragUpBy(device.displayHeight - dock.top)
            settle(extraMs = 300)
            device.findObject(selector)?.let { return it }
        }
        fail("Nothing on screen matches $selector, nor under the dock")
    }

    /** Taps the last view matching [selector], such as a dialog's button named like the one that opened the dialog. */
    fun tapLast(selector: BySelector) {
        try {
            tap(lastOf(selector))
        } catch (_: StaleObjectException) {
            tap(lastOf(selector))
        }
    }

    private fun lastOf(selector: BySelector): UiObject2 {
        find(selector)
        return device.findObjects(selector).lastOrNull() ?: fail("Nothing on screen matches $selector")
    }

    /**
     * Taps [selector] until [done] holds. A tap made while the screen is still sliding in, which a busy device draws
     * slowly, lands where the control was a moment before and does nothing.
     */
    fun tapUntil(selector: BySelector, done: SearchCondition<Boolean>, timeoutMs: Long = TIMEOUT_MS) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (true) {
            try {
                device.findObject(selector)?.let(::tap)
            } catch (_: StaleObjectException) {
                // Redrawn between finding and tapping it; the next attempt finds it again.
            }
            if (device.wait(done, TAP_ATTEMPT_MS)) return
            if (SystemClock.uptimeMillis() > deadline) fail("Tapping $selector never took effect")
        }
    }

    /** Waits until the UI is idle and covers loaded over the network have had time to fade in. */
    fun settle(extraMs: Long = 1_500) {
        device.waitForIdle()
        SystemClock.sleep(extraMs)
    }

    /** Taps the point at the given fractions of the screen's width and height. */
    fun tapAt(x: Float, y: Float) {
        tapPoint(device.displayWidth * x, device.displayHeight * y)
    }

    fun tap(view: UiObject2) {
        var bounds = steadyBounds(view)
        if (revealAboveDock(view, bounds)) bounds = steadyBounds(view)
        tapPoint(bounds.centerX().toFloat(), bounds.centerY().toFloat())
    }

    /**
     * Scrolls [view] up from under the home dock if a tap at its centre would land on the dock, since screens scroll
     * on under it. The dock covers such a view only partly, as it does a row wider than itself, whose bounds take in
     * the ends on both sides. Whether it is in the way depends on how long the screen's list is and how wide the dock,
     * which change with the UI, so no scene has to know. Returns whether it scrolled.
     */
    private fun revealAboveDock(view: UiObject2, at: Rect): Boolean {
        var bounds = at
        var scrolled = false
        repeat(REVEAL_ATTEMPTS) {
            val dock = device.findObject(By.res(FLOATING_DOCK))?.visibleBounds ?: return scrolled
            if (dock.contains(bounds) || !dock.contains(bounds.centerX(), bounds.centerY())) return scrolled
            dragUpBy(bounds.bottom - dock.top + dock.height())
            scrolled = true
            bounds = view.visibleBounds
        }
        return scrolled
    }

    /**
     * Where [view] comes to rest. Kit lists animate their rows into place, and a sheet resizes to a filtered list, so a
     * row found the moment it appears may still be moving, and a tap at where it was lands on nothing. One that never
     * holds still, such as a control that follows playback, is tapped where it is when the wait runs out.
     */
    private fun steadyBounds(view: UiObject2): Rect {
        val deadline = SystemClock.uptimeMillis() + STEADY_TIMEOUT_MS
        var bounds = view.visibleBounds
        var steadySamples = 0
        while (steadySamples < STEADY_SAMPLES && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(STEADY_INTERVAL_MS)
            val next = view.visibleBounds
            steadySamples = if (next == bounds) steadySamples + 1 else 0
            bounds = next
        }
        return bounds
    }

    /**
     * Taps with the finger's press and release sent together. UiAutomator's taps wait for the app to handle the press
     * before releasing, and an app whose main thread stalls on a frame in between, as one drawing in software does for
     * half a second and more, finds the finger held past its long-press timeout and turns nothing. Sent together, both
     * wait in the app's input channel, and the app reads them in one go.
     */
    private fun tapPoint(x: Float, y: Float) {
        val downTime = SystemClock.uptimeMillis()
        touch(MotionEvent.ACTION_DOWN, downTime, x, y, sync = false)
        touch(MotionEvent.ACTION_UP, downTime, x, y, sync = true)
    }

    /** Injects one touch event; [sync] waits until the app has handled it. */
    fun touch(action: Int, downTime: Long, x: Float, y: Float, sync: Boolean = true) {
        val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        try {
            if (!instrumentation.uiAutomation.injectInputEvent(event, sync)) fail("The touch could not be injected")
        } finally {
            event.recycle()
        }
    }

    /**
     * Taps the point at the given fractions of the screen until [shown] is on screen, as for a reader's or player's
     * bars, which a tap shows and hides in turn. A busy device can draw them before it reports them, and a tap then
     * hides them again, so each tap waits to see whether the last one took.
     */
    fun tapToShow(x: Float, y: Float, shown: BySelector) {
        val deadline = SystemClock.uptimeMillis() + TIMEOUT_MS
        while (!device.wait(Until.hasObject(shown), TOGGLE_WAIT_MS)) {
            if (SystemClock.uptimeMillis() > deadline) fail("Tapping never showed $shown")
            tapAt(x, y)
        }
    }

    /** As [tapToShow], until [shown] is gone. */
    fun tapToHide(x: Float, y: Float, shown: BySelector) {
        val deadline = SystemClock.uptimeMillis() + TIMEOUT_MS
        while (!device.wait(Until.gone(shown), TOGGLE_WAIT_MS)) {
            if (SystemClock.uptimeMillis() > deadline) fail("Tapping never hid $shown")
            tapAt(x, y)
        }
    }

    fun awaitGone(selector: BySelector, timeoutMs: Long = TIMEOUT_MS) {
        if (!device.wait(Until.gone(selector), timeoutMs)) fail("$selector stayed on screen")
    }

    fun fail(message: String): Nothing {
        // Kept apart from the captures so a failed run never publishes it.
        shell("mkdir -p $FAILURE_DIRECTORY")
        val name = System.currentTimeMillis().toString()
        shell("screencap -p $FAILURE_DIRECTORY/$name.png")
        saveFailureReport(name)
        error(message)
    }

    /**
     * Saves the screen once two screenshots in a row are the same. Idle as the app may be, the system can still be
     * moving its bars into place after a window change, which would shift the whole still by a few pixels.
     */
    fun capture(name: String) {
        settle()
        shell("mkdir -p $CAPTURE_DIRECTORY")
        val still = File("$CAPTURE_DIRECTORY/$name.png")
        var previous: ByteArray? = null
        repeat(STILL_ATTEMPTS) {
            shell("screencap -p ${still.path}")
            val current = still.readBytes()
            if (current.contentEquals(previous)) return
            previous = current
            SystemClock.sleep(STILL_INTERVAL_MS)
        }
        fail("The screen never held still for $name")
    }

    /**
     * Records the screen to `[name].mp4` in [CAPTURE_DIRECTORY] while [block] runs. With a [slowdown] above 1, [block]
     * plays what it shows that many times slower than the clip should, and `[name].slowdown` tells the pipeline to speed
     * the clip up again.
     */
    fun record(name: String, slowdown: Int = 1, block: () -> Unit) {
        shell("mkdir -p $CAPTURE_DIRECTORY")
        if (slowdown > 1) writeDeviceFile("$CAPTURE_DIRECTORY/$name.slowdown", "$slowdown")
        // UiDevice.executeShellCommand waits for the command to exit; this one has to keep running alongside [block].
        // Without a time limit, as a slowed clip outlasts the default 3 minutes.
        instrumentation.uiAutomation
            .executeShellCommand("screenrecord --time-limit 0 --bit-rate 8000000 $CAPTURE_DIRECTORY/$name.mp4")
            .close()
        SystemClock.sleep(RECORDER_START_MS)
        try {
            block()
        } finally {
            shell("pkill -INT screenrecord")
            // The recorder writes the file's index after it stops.
            SystemClock.sleep(RECORDER_START_MS)
        }
    }

    /** Runs [block] with the system animator scale at [scale], which the app reads when its screens open. */
    fun <T> withAnimatorScale(scale: Float, block: () -> T): T {
        shell("settings put global animator_duration_scale $scale")
        try {
            return block()
        } finally {
            shell("settings put global animator_duration_scale 1")
        }
    }

    private const val TIMEOUT_MS = 30_000L

    // The kit's test tag on the home dock.
    private const val FLOATING_DOCK = "floating_dock"

    // Fractions of the screen's height a scroll drags between, and the 5 ms steps it takes, as fast as a scroll by
    // UiAutomator's default speed.
    private const val SCROLL_FROM = 0.7f
    private const val SCROLL_TO = 0.3f
    private const val SCROLL_STEPS = 50

    // The status bar's band, whose swipes pull the notification shade, and gesture navigation's 32 dp band with room
    // for the slop the system allows a swipe.
    private const val TOP_SYSTEM_GESTURES_DP = 72
    private const val BOTTOM_SYSTEM_GESTURES_DP = 48
    private const val DEFAULT_GESTURE_MARGIN = 0.1f
    private const val MIN_GESTURE_HEIGHT_PX = 100
    private const val TAP_ATTEMPT_MS = 2_000L

    // Further apart than the 20 ms within which input resampling blends touch moves into positions between them, so the
    // list reads each step as the whole pixels it was sent as and rounds the same way every run.
    private const val DRAG_STEP_MS = 30L
    private const val DRAG_STEP_PX = 10f
    private const val DRAG_HOLD_MOVES = 6
    private const val DRAG_HOLD_INTERVAL_MS = 50L
    private const val REVEAL_ATTEMPTS = 3
    private const val STEADY_INTERVAL_MS = 100L
    private const val STEADY_SAMPLES = 2
    private const val STEADY_TIMEOUT_MS = 2_000L
    private const val TOGGLE_WAIT_MS = 2_000L
    private const val RECORDER_START_MS = 1_000L
    private const val STILL_ATTEMPTS = 20
    private const val STILL_INTERVAL_MS = 500L
}
