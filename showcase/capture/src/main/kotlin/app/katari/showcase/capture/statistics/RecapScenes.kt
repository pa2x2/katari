package app.katari.showcase.capture.statistics

import android.os.SystemClock
import android.view.MotionEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.Until
import app.katari.showcase.capture.ShowcaseDevice.capture
import app.katari.showcase.capture.ShowcaseDevice.device
import app.katari.showcase.capture.ShowcaseDevice.fail
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.record
import app.katari.showcase.capture.ShowcaseDevice.tap
import app.katari.showcase.capture.ShowcaseDevice.tapAt
import app.katari.showcase.capture.ShowcaseDevice.touch
import app.katari.showcase.capture.ShowcaseDevice.withAnimatorScale
import app.katari.showcase.capture.tapAction
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

/**
 * The year's recap, played as a story. The clip taps through its opening pages faster than they advance on their own;
 * the share image is written to the app's cache, where the pipeline picks it up.
 *
 * The emulator draws in software, a few frames a second of the recap's moving gradients, so the clip is recorded
 * [CLIP_SLOWDOWN] times slower, the benchmark build's recap following the animator scale, and sped up again.
 */
@RunWith(AndroidJUnit4::class)
class RecapScenes {
    @Test
    fun clip() {
        withAnimatorScale(CLIP_SLOWDOWN.toFloat()) {
            // The recap reads the scale as it opens.
            openRecap()
            record("recap", slowdown = CLIP_SLOWDOWN) {
                SystemClock.sleep(2_000L * CLIP_SLOWDOWN)
                repeat(CLIP_PAGES) {
                    tapAt(0.85f, 0.5f)
                    SystemClock.sleep(4_000L * CLIP_SLOWDOWN)
                }
            }
        }
    }

    /**
     * Pages keep moving after their entrance, so a still would catch them somewhere different on every run. With
     * animations removed, the recap opens with its entrances finished and its motion at rest, and holding a finger on
     * the page stops the progress bar above it, which [onPage] does from right after each page turns.
     */
    @Test
    fun pages() {
        withAnimatorScale(0f) {
            openRecap()
            onPage(By.text(Pattern.compile("your #1", Pattern.CASE_INSENSITIVE))) { capture("recap-top") }
            // The summary is shown as the picture it shares rather than as a screenshot.
            onPage(By.text("Watch again")) {}

            tap(By.text("Share"))
            // The image is written before the share sheet opens, and the test ending earlier would leave it half
            // written.
            find(By.pkg(SHARE_SHEET))
            device.pressBack()
        }
    }

    private fun openRecap() {
        openStatistics()
        // The dashboard offers a recap only until it has been watched once.
        tapAction("Recaps")
        tap(By.text("2026 so far"))
        find(By.text("Tap to start"))
    }

    /**
     * Holds a finger on the page's footer while [block] runs, which pauses the story. A long press elsewhere could
     * land on a title, which offers to hide it, and releasing after one never counts as a tap.
     */
    private fun <T> whileHolding(block: () -> T): T {
        val x = device.displayWidth * 0.3f
        val y = device.displayHeight * FOOTER_Y
        val downTime = SystemClock.uptimeMillis()
        touch(MotionEvent.ACTION_DOWN, downTime, x, y)
        try {
            return block()
        } finally {
            touch(MotionEvent.ACTION_UP, downTime, x, y)
        }
    }

    /**
     * Turns pages until [page] is on screen, then runs [block] on it. The story stays paused under a finger from right
     * after each tap, so every page is read, and the last one shot, at the same point of its progress whatever the
     * device's speed. A busy device reports a page piece by piece and can take seconds to, so a page is read once its
     * text has held still, and the tap before it is seen to have taken by that text changing.
     */
    private fun onPage(page: BySelector, block: () -> Unit) {
        var shown: String? = null
        repeat(MAX_PAGES) {
            val reached = whileHolding {
                shown?.let { awaitTurnFrom(it, page) }
                shown = settledPageText()
                device.hasObject(page).also { if (it) block() }
            }
            if (reached) return
            tapAt(0.85f, 0.5f)
        }
        fail("The recap never reached $page")
    }

    private fun awaitTurnFrom(shown: String, page: BySelector) {
        val deadline = SystemClock.uptimeMillis() + PAGE_TURN_TIMEOUT_MS
        while (pageText() == shown) {
            if (SystemClock.uptimeMillis() > deadline) fail("The recap never turned past its page while seeking $page")
            SystemClock.sleep(PAGE_POLL_MS)
        }
    }

    private fun settledPageText(): String {
        var text = pageText()
        while (true) {
            SystemClock.sleep(PAGE_SETTLE_MS)
            val again = pageText()
            if (again == text) return text
            text = again
        }
    }

    private fun pageText(): String {
        while (true) {
            try {
                return device.findObjects(By.text(ANY_TEXT)).joinToString("\n") { it.text }
            } catch (_: StaleObjectException) {
                // A page turned while it was read; reading again gets the new one whole.
            }
        }
    }

    private companion object {
        const val CLIP_PAGES = 5

        // At about 3 frames a second on CI, 8 times slower comes out near 25 frames a second.
        const val CLIP_SLOWDOWN = 8
        const val MAX_PAGES = 20
        const val PAGE_SETTLE_MS = 1_500L
        const val PAGE_TURN_TIMEOUT_MS = 30_000L
        const val PAGE_POLL_MS = 250L
        val ANY_TEXT: Pattern = Pattern.compile(".+", Pattern.DOTALL)

        // The footer every page ends with, as a fraction of the screen's height.
        const val FOOTER_Y = 0.87f
        const val SHARE_SHEET = "com.android.intentresolver"
    }
}
