package app.katari.showcase.capture

import android.content.Intent
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.io.File

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

    /**
     * Scrolls the screen's list down until [selector] is on screen. The list is looked up again for every step, since
     * a list that reorders while it loads replaces the view a step began with.
     */
    fun scrollTo(selector: BySelector, timeoutMs: Long = TIMEOUT_MS): UiObject2 {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            device.findObject(selector)?.let { return it }
            try {
                device.findObject(By.scrollable(true))?.scroll(Direction.DOWN, 0.5f)
            } catch (_: StaleObjectException) {
                // Replaced between finding and scrolling it; the next step finds the new one.
            }
            settle(extraMs = 300)
        }
        fail("Scrolling never reached $selector")
    }

    fun tap(selector: BySelector, timeoutMs: Long = TIMEOUT_MS) {
        try {
            find(selector, timeoutMs).click()
        } catch (_: StaleObjectException) {
            // Redrawn between finding and tapping it, as controls that follow playback are.
            find(selector, timeoutMs).click()
        }
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

    fun awaitGone(selector: BySelector, timeoutMs: Long = TIMEOUT_MS) {
        if (!device.wait(Until.gone(selector), timeoutMs)) fail("$selector stayed on screen")
    }

    fun fail(message: String): Nothing {
        // Kept apart from the captures so a failed run never publishes it.
        shell("mkdir -p $FAILURE_DIRECTORY")
        shell("screencap -p $FAILURE_DIRECTORY/${System.currentTimeMillis()}.png")
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
    private const val STILL_ATTEMPTS = 20
    private const val STILL_INTERVAL_MS = 500L
}
