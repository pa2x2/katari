package app.katari.showcase.capture.browse

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import app.katari.showcase.capture.HomeTab
import app.katari.showcase.capture.ShowcaseDevice.capture
import app.katari.showcase.capture.ShowcaseDevice.device
import app.katari.showcase.capture.ShowcaseDevice.dragUp
import app.katari.showcase.capture.ShowcaseDevice.enterText
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.scrollTo
import app.katari.showcase.capture.ShowcaseDevice.scrollToTop
import app.katari.showcase.capture.ShowcaseDevice.settle
import app.katari.showcase.capture.ShowcaseDevice.tap
import app.katari.showcase.capture.openProfile
import app.katari.showcase.capture.openTab
import app.katari.showcase.capture.tapAction
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowseScenes {
    /** The showcase sources match genres, so one word finds comics in two languages, books, and a film. */
    @Test
    fun globalSearch() {
        openProfile("Personal")
        openTab(HomeTab.Browse)
        tap(By.text("Sources"))
        tapAction("Global search")
        enterText(By.clazz("android.widget.EditText"), "Fantasy")
        device.pressEnter()
        device.pressBack()
        tap(By.text("All"))
        // Sources move to the top as their results arrive, so the list settles once the slowest ones are in.
        find(By.text("Sintel"))
        scrollTo(By.textStartsWith("The War of the Worlds"))
        settle()
        // The results' top, then one held drag past the film to the comics in both languages and the books under them;
        // where a swipe stopped differs from run to run.
        scrollToTop()
        dragUp(RESULTS_FRAMING)
        capture("global-search")
    }

    @Test
    fun feed() {
        openProfile("Personal")
        openTab(HomeTab.Browse)
        tap(By.text("Feeds"))
        tap(By.text("Add feed"))
        tap(By.text("Project Gutenberg"))
        tap(By.text("Popular"))
        find(By.text("Treasure Island"))
        capture("feed")
    }

    /** The showcase extension supplies every kind of title, which its row shows. */
    @Test
    fun extensions() {
        openProfile("Personal")
        openTab(HomeTab.Browse)
        tap(By.text("Extensions"))
        find(By.text("Showcase"))
        capture("extensions")
    }

    @Test
    fun migration() {
        openProfile("Personal")
        openTab(HomeTab.Browse)
        tap(By.text("Migrate"))
        find(By.text("Project Gutenberg"))
        capture("migration")
    }

    private companion object {
        // Of the screen's height: the film's source goes under the filter chips and the next source takes its place.
        const val RESULTS_FRAMING = 0.32f
    }
}
