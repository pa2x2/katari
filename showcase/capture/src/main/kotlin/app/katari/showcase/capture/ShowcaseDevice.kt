package app.katari.showcase.capture

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

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

    fun tap(selector: BySelector, timeoutMs: Long = TIMEOUT_MS) {
        find(selector, timeoutMs).click()
    }

    /** Waits until the UI is idle and covers loaded over the network have had time to fade in. */
    fun settle(extraMs: Long = 1_500) {
        device.waitForIdle()
        SystemClock.sleep(extraMs)
    }

    /** Taps the point at the given fractions of the screen's width and height. */
    fun tapAt(x: Float, y: Float) {
        device.click((device.displayWidth * x).toInt(), (device.displayHeight * y).toInt())
    }

    /**
     * Runs [action], then waits until the middle of the screen, between the top and bottom bars, differs from how it
     * looked before and holds still, for changes the app draws without exposing them to accessibility. The screen is
     * sampled before [action] because the change may already be drawn by the time it returns.
     */
    fun awaitScreenChange(timeoutMs: Long = CHANGE_TIMEOUT_MS, action: () -> Unit) {
        val baseline = screenSample()
        action()
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        var previous = baseline
        var changed = false
        while (SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(POLL_MS)
            val current = screenSample()
            if (!changed) {
                changed = !current.contentEquals(baseline)
            } else if (current.contentEquals(previous)) {
                return
            }
            previous = current
        }
        fail(if (changed) "The screen never held still" else "The screen never changed")
    }

    private fun fail(message: String): Nothing {
        // Kept apart from the captures so a failed run never publishes it.
        shell("mkdir -p $FAILURE_DIRECTORY")
        shell("screencap -p $FAILURE_DIRECTORY/${System.currentTimeMillis()}.png")
        error(message)
    }

    /** The screen at a sixteenth of its size, from 15% of its height down to two thirds of it. */
    private fun screenSample(): IntArray {
        // UiAutomation screenshots stop following the screen once apps draw through Vulkan; screencap reads what
        // SurfaceFlinger composes, like the captures themselves.
        shell("screencap -p $SAMPLE_FILE")
        val options = BitmapFactory.Options().apply { inSampleSize = 16 }
        val screenshot = BitmapFactory.decodeFile(SAMPLE_FILE, options) ?: error("The screen could not be captured")
        try {
            val top = screenshot.height * 15 / 100
            val rows = screenshot.height * 2 / 3 - top
            return IntArray(screenshot.width * rows).also {
                screenshot.getPixels(it, 0, screenshot.width, 0, top, screenshot.width, rows)
            }
        } finally {
            screenshot.recycle()
        }
    }

    fun capture(name: String) {
        settle()
        shell("mkdir -p $CAPTURE_DIRECTORY")
        shell("screencap -p $CAPTURE_DIRECTORY/$name.png")
    }

    /** Records the screen to `[name].mp4` in [CAPTURE_DIRECTORY] while [block] runs. */
    fun record(name: String, block: () -> Unit) {
        shell("mkdir -p $CAPTURE_DIRECTORY")
        // UiDevice.executeShellCommand waits for the command to exit; this one has to keep running alongside [block].
        instrumentation.uiAutomation
            .executeShellCommand("screenrecord --bit-rate 8000000 $CAPTURE_DIRECTORY/$name.mp4")
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

    private const val TIMEOUT_MS = 30_000L
    private const val RECORDER_START_MS = 1_000L
    private const val CHANGE_TIMEOUT_MS = 180_000L
    private const val POLL_MS = 1_500L
    private const val SAMPLE_FILE = "/data/local/tmp/showcase-sample.png"
}
