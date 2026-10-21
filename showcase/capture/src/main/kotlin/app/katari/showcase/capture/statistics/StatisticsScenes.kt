package app.katari.showcase.capture.statistics

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import app.katari.showcase.capture.HomeTab
import app.katari.showcase.capture.ShowcaseDevice.capture
import app.katari.showcase.capture.ShowcaseDevice.dragUp
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.list
import app.katari.showcase.capture.ShowcaseDevice.settle
import app.katari.showcase.capture.ShowcaseDevice.tap
import app.katari.showcase.capture.openProfile
import app.katari.showcase.capture.openTab
import app.katari.showcase.capture.tapAction
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
        // The year's figures load after the tap, and cards that grow above a list being dragged move what is in view.
        settle()
        dragUp(0.22f)
        capture("statistics-year")
    }

    /** The daily goal sits at the end of the sheet, below the cards. */
    @Test
    fun customize() {
        openStatistics()
        tapAction("Customize")
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
    openTab(HomeTab.More)
    tap(By.text("Statistics"))
    find(By.text("Time spent"))
}
