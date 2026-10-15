package app.katari.showcase.capture.statistics

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import app.katari.showcase.capture.ShowcaseDevice.capture
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.list
import app.katari.showcase.capture.ShowcaseDevice.tap
import app.katari.showcase.capture.openProfile
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatisticsScenes {
    @Test
    fun dashboard() {
        openStatistics()
        // The range persists between visits.
        tap(By.text("30D"))
        capture("statistics")

        tap(By.text("1Y"))
        // Released slower than a fling, the list stops where the finger does instead of a little further each run.
        list().scroll(Direction.DOWN, 0.4f, SCROLL_PX_PER_SECOND)
        capture("statistics-year")
    }

    /** The daily goal sits at the end of the sheet, below the cards. */
    @Test
    fun customize() {
        openStatistics()
        tap(By.desc("More options"))
        tap(By.text("Customize"))
        val sheet = list()
        do {
            val more = sheet.scroll(Direction.DOWN, 1f, SCROLL_PX_PER_SECOND)
        } while (more)
        capture("statistics-customize")
    }

    private companion object {
        const val SCROLL_PX_PER_SECOND = 100
    }
}

internal fun openStatistics() {
    openProfile("Personal")
    tap(By.text("More"))
    tap(By.text("Statistics"))
    find(By.text("Time spent"))
}
