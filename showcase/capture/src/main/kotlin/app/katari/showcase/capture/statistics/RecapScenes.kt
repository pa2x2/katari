package app.katari.showcase.capture.statistics

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Until
import app.katari.showcase.capture.ShowcaseDevice.capture
import app.katari.showcase.capture.ShowcaseDevice.device
import app.katari.showcase.capture.ShowcaseDevice.fail
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.record
import app.katari.showcase.capture.ShowcaseDevice.shell
import app.katari.showcase.capture.ShowcaseDevice.tap
import app.katari.showcase.capture.ShowcaseDevice.tapAt
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

/**
 * The year's recap, played as a story. The clip taps through its opening pages faster than they advance on their own;
 * the share image is written to the app's cache, where the pipeline picks it up.
 */
@RunWith(AndroidJUnit4::class)
class RecapScenes {
    @Test
    fun clip() {
        openRecap()
        record("recap") {
            SystemClock.sleep(2_000)
            repeat(CLIP_PAGES) {
                tapAt(0.85f, 0.5f)
                SystemClock.sleep(4_000)
            }
        }
    }

    /**
     * Pages keep moving after their entrance, so a still would catch them somewhere different on every run. With
     * animations removed, the recap opens with its entrances finished and its motion at rest, and holding a finger on
     * the page stops the progress bar above it.
     */
    @Test
    fun pages() {
        shell("settings put global animator_duration_scale 0")
        try {
            openRecap()
            advanceTo(By.text(Pattern.compile("your #1", Pattern.CASE_INSENSITIVE)))
            whileHolding { capture("recap-top") }
            // The summary is shown as the picture it shares rather than as a screenshot.
            advanceTo(By.text("Watch again"))

            tap(By.text("Share"))
            // The image is written before the share sheet opens, and the test ending earlier would leave it half
            // written.
            find(By.pkg(SHARE_SHEET))
            device.pressBack()
        } finally {
            shell("settings put global animator_duration_scale 1")
        }
    }

    private fun openRecap() {
        openStatistics()
        // The dashboard offers a recap only until it has been watched once.
        tap(By.desc("More options"))
        tap(By.text("Recaps"))
        tap(By.text("2026 so far"))
        find(By.text("Tap to start"))
    }

    /**
     * Holds a finger on the page's footer while [block] runs, which pauses the story. A long press elsewhere could
     * land on a title, which offers to hide it, and releasing after one never counts as a tap.
     */
    private fun whileHolding(block: () -> Unit) {
        val x = device.displayWidth * 0.3f
        val y = device.displayHeight * FOOTER_Y
        val downTime = SystemClock.uptimeMillis()
        touch(MotionEvent.ACTION_DOWN, downTime, x, y)
        try {
            block()
        } finally {
            touch(MotionEvent.ACTION_UP, downTime, x, y)
        }
    }

    private fun touch(action: Int, downTime: Long, x: Float, y: Float) {
        val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        try {
            check(InstrumentationRegistry.getInstrumentation().uiAutomation.injectInputEvent(event, true)) {
                "The touch could not be injected"
            }
        } finally {
            event.recycle()
        }
    }

    /** Each page is given time to appear in the accessibility tree before moving on, or a tap could skip past it. */
    private fun advanceTo(page: BySelector) {
        repeat(MAX_PAGES) {
            if (device.wait(Until.hasObject(page), PAGE_WAIT_MS)) return
            tapAt(0.85f, 0.5f)
        }
        fail("The recap never reached $page")
    }

    private companion object {
        const val CLIP_PAGES = 5
        const val MAX_PAGES = 20
        const val PAGE_WAIT_MS = 2_000L

        // The footer every page ends with, as a fraction of the screen's height.
        const val FOOTER_Y = 0.87f
        const val SHARE_SHEET = "com.android.intentresolver"
    }
}
