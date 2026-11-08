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
import java.util.regex.Pattern
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The app under capture and the device it runs on. Scenes drive it through the UI only, record stills to
 * [CAPTURE_DIRECTORY] with the shell's screencap, and the pipeline pulls that directory afterwards. The pipeline passes
 * the app's package as the `targetAppId` instrumentation argument.
 */
internal object ShowcaseDevice {
    const val CAPTURE_DIRECTORY = "/data/local/tmp/showcase"
    const val FAILURE_DIRECTORY = "/data/local/tmp/showcase-failures"

    // The pipeline watches for it while a scene with another alongside it runs (tools/showcase/pipeline/scenes.ts).
    private const val READY_SIGNAL = "/data/local/tmp/showcase-ready"

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
     * Scrolls the screen back to its top through the middle of the screen, as [scrollTo] goes down, until a swipe moves
     * nothing. A swipe glides on and stops wherever its speed runs out, a few pixels apart from run to run, but every
     * run stops at the top, so a still taken there or [dragUp] from there comes out the same.
     */
    fun scrollToTop(timeoutMs: Long = TIMEOUT_MS) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        val x = device.displayWidth / 2
        while (SystemClock.uptimeMillis() < deadline) {
            val before = textBounds()
            device.swipe(
                x,
                (device.displayHeight * SCROLL_TO).toInt(),
                x,
                (device.displayHeight * SCROLL_FROM).toInt(),
                SCROLL_STEPS,
            )
            settle(extraMs = 300)
            if (textBounds() == before) return
        }
        fail("Scrolling never reached the top")
    }

    private fun textBounds(): List<Rect> = device.findObjects(By.text(ANY_TEXT)).map { it.visibleBounds }

    /**
     * The scrollable matching [selector], with its gestures kept out of the bands along the top and bottom edges where
     * the system takes a swipe as its own: one starting at the bottom edge opens recents in place of scrolling.
     * Elsewhere they keep UiAutomator's default margin of a tenth of the size, so scrolls go as far as they always did.
     *
     * UiAutomator returns the deepest match first, which on a list of rows that scroll sideways is one of the rows. A
     * vertical gesture inside a row is too short for the list around it to take reliably, and CI saw such a fling open
     * the result under it, so the match is widened to the outermost scrollable holding it.
     */
    fun list(selector: BySelector = By.scrollable(true)): UiObject2 {
        val match = find(selector)
        val list = generateSequence(match.parent) { it.parent }.lastOrNull { it.isScrollable } ?: match
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

    /**
     * Presses the screen point ([startX], [startY]), holds it past the long-press timeout, and drags to ([endX], [endY])
     * before letting go, as a reader selects a passage. The drag steps as far apart as [dragUp]'s, so the selection ends
     * on the same word every run.
     */
    fun pressAndDrag(startX: Int, startY: Int, endX: Int, endY: Int) {
        val downTime = SystemClock.uptimeMillis()
        touch(MotionEvent.ACTION_DOWN, downTime, startX.toFloat(), startY.toFloat())
        SystemClock.sleep(LONG_PRESS_HOLD_MS)
        val steps = maxOf(abs(endX - startX), abs(endY - startY)) / DRAG_STEP_PX.toInt()
        for (step in 1..steps) {
            SystemClock.sleep(DRAG_STEP_MS)
            val x = startX + (endX - startX) * step / steps
            val y = startY + (endY - startY) * step / steps
            touch(MotionEvent.ACTION_MOVE, downTime, x.toFloat(), y.toFloat())
        }
        repeat(DRAG_HOLD_MOVES) {
            SystemClock.sleep(DRAG_HOLD_INTERVAL_MS)
            touch(MotionEvent.ACTION_MOVE, downTime, endX.toFloat(), endY.toFloat())
        }
        touch(MotionEvent.ACTION_UP, downTime, endX.toFloat(), endY.toFloat())
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

    /** Closes the on-screen keyboard if typing brought it up, as it covers the bottom of the screen. */
    fun hideKeyboard() {
        if ("mInputShown=true" in shell("dumpsys input_method")) {
            device.pressBack()
            settle()
        }
    }

    /** A text field by the label it shows, which Compose lays out inside the field. */
    fun textField(label: String): BySelector = By.clazz("android.widget.EditText").hasDescendant(By.text(label))

    fun enterText(selector: BySelector, text: String) {
        try {
            find(selector).text = text
        } catch (_: StaleObjectException) {
            // Redrawn between finding and typing into it, as a field on a screen still sliding in is.
            find(selector).text = text
        }
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
        refreshGestureHandle()
        shell("mkdir -p $CAPTURE_DIRECTORY")
        if (!heldStill(File("$CAPTURE_DIRECTORY/$name.png"))) fail("The screen never held still for $name")
    }

    /**
     * Has the gesture handle take its colour from the screen as it is now. The launcher draws the handle light or dark
     * by the brightness behind it, but changes it only once that brightness moves by more than a step from what it last
     * went by, so a screen that brightened gradually, as covers fading in do, keeps the colour the handle took partway,
     * which differs from run to run. A launcher started afresh goes by its first look. A screen that hides the
     * navigation bar, as the readers do, shows no handle, and the launcher looks at nothing until the bar returns.
     */
    private fun refreshGestureHandle() {
        if (!navigationBarShown()) return
        val home = "-a android.intent.action.MAIN -c android.intent.category.HOME"
        val launcher = shell("cmd package resolve-activity --brief $home")
            .lines()
            .last { it.isNotBlank() }
            .substringBefore('/')
        shell("am force-stop $launcher")
        val deadline = SystemClock.uptimeMillis() + TIMEOUT_MS
        while ("mSamplingListenerRegistered: true" !in shell("dumpsys activity service $launcher")) {
            if (SystemClock.uptimeMillis() > deadline) fail("The launcher never drew the gesture handle again")
            SystemClock.sleep(HANDLE_POLL_MS)
        }
        // The first look comes with the next frame the system composes, or once it has been idle for a moment.
        SystemClock.sleep(HANDLE_SAMPLE_MS)
    }

    /** Whether the window that draws the navigation bar, and the gesture handle in it, is on screen. */
    private fun navigationBarShown(): Boolean =
        shell("dumpsys window windows")
            .split(WINDOW_HEADER)
            .any { " ty=NAVIGATION_BAR " in it && "isVisible=true" in it }

    /**
     * Saves the screen as it is this moment, for what never holds still, such as a countdown, which [capture] would wait
     * out. Taken right after [awaitPromptly], it shows what that waited for.
     */
    fun captureNow(name: String) {
        shell("mkdir -p $CAPTURE_DIRECTORY")
        shell("screencap -p $CAPTURE_DIRECTORY/$name.png")
    }

    /**
     * Waits for [selector] to show, looking as often as the screen can be read rather than at UiAutomator's usual
     * second, for a still taken the moment something changes, such as one count of a countdown.
     */
    fun awaitPromptly(selector: BySelector, timeoutMs: Long = TIMEOUT_MS) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (!device.hasObject(selector)) {
            if (SystemClock.uptimeMillis() > deadline) fail("Nothing on screen matches $selector")
        }
    }

    /** Tells the pipeline the scene is ready for the desktop scene that runs alongside it. */
    fun signalReady() = writeDeviceFile(READY_SIGNAL, "")

    /**
     * Waits until the screen holds still, as [capture] does, for a gesture that must land on what is drawn now: a
     * reader can still be moving its page into place after it shows its progress.
     */
    fun awaitStill() {
        settle()
        val probe = File(STILL_PROBE)
        try {
            if (!heldStill(probe)) fail("The screen never held still")
        } finally {
            shell("rm -f ${probe.path}")
        }
    }

    /** Takes screenshots into [file] until two in a row are the same; false when the screen never holds still. */
    private fun heldStill(file: File): Boolean {
        var previous: ByteArray? = null
        repeat(STILL_ATTEMPTS) {
            shell("screencap -p ${file.path}")
            val current = file.readBytes()
            if (current.contentEquals(previous)) return true
            previous = current
            SystemClock.sleep(STILL_INTERVAL_MS)
        }
        return false
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
    private val ANY_TEXT = Pattern.compile(".+", Pattern.DOTALL)

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

    // Well past the 400 ms after which a held finger becomes a long press.
    private const val LONG_PRESS_HOLD_MS = 1_000L
    private const val REVEAL_ATTEMPTS = 3
    private const val STEADY_INTERVAL_MS = 100L
    private const val STEADY_SAMPLES = 2
    private const val STEADY_TIMEOUT_MS = 2_000L
    private const val TOGGLE_WAIT_MS = 2_000L
    private const val RECORDER_START_MS = 1_000L
    private const val STILL_ATTEMPTS = 20
    private const val STILL_INTERVAL_MS = 500L
    private const val HANDLE_POLL_MS = 200L
    private const val HANDLE_SAMPLE_MS = 1_000L
    private val WINDOW_HEADER = Regex("^  Window #", RegexOption.MULTILINE)

    // Outside CAPTURE_DIRECTORY, which the pipeline publishes whole.
    private const val STILL_PROBE = "/data/local/tmp/showcase-still.png"
}
