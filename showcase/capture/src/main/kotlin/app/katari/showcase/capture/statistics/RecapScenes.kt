package app.katari.showcase.capture.statistics

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
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
     * animations removed, the recap opens with its entrances finished and its motion at rest.
     */
    @Test
    fun pages() {
        shell("settings put global animator_duration_scale 0")
        try {
            openRecap()
            advanceTo(By.text(Pattern.compile("your #1", Pattern.CASE_INSENSITIVE)))
            capture("recap-top")
            advanceTo(By.text("Watch again"))
            capture("recap-summary")

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
        const val SHARE_SHEET = "com.android.intentresolver"
    }
}
