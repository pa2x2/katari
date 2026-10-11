package app.katari.showcase.capture.statistics

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import app.katari.showcase.capture.ShowcaseDevice.capture
import app.katari.showcase.capture.ShowcaseDevice.device
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.record
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
    fun recap() {
        openStatistics()
        // The dashboard offers a recap only until it has been watched once.
        tap(By.desc("More options"))
        tap(By.text("Recaps"))
        tap(By.text("2026 so far"))
        find(By.text("Tap to start"))

        record("recap") {
            SystemClock.sleep(2_000)
            repeat(CLIP_PAGES) {
                tapAt(0.85f, 0.5f)
                SystemClock.sleep(4_000)
            }
        }

        // The clip ends on the month-by-month page, two after the top title.
        repeat(2) { tapAt(0.15f, 0.5f) }
        find(By.text(Pattern.compile("your #1", Pattern.CASE_INSENSITIVE)))
        capture("recap-top")

        var pages = 0
        while (device.findObject(By.text("Watch again")) == null) {
            check(pages++ < MAX_PAGES) { "The recap never reached its summary" }
            tapAt(0.85f, 0.5f)
            SystemClock.sleep(800)
        }
        capture("recap-summary")
        tap(By.text("Share"))
    }

    private companion object {
        const val CLIP_PAGES = 5
        const val MAX_PAGES = 20
    }
}
