package app.katari.showcase.capture.browse

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import app.katari.showcase.capture.HomeTab
import app.katari.showcase.capture.ShowcaseDevice.capture
import app.katari.showcase.capture.ShowcaseDevice.device
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.list
import app.katari.showcase.capture.ShowcaseDevice.scrollTo
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
        find(By.clazz("android.widget.EditText")).text = "Fantasy"
        device.pressEnter()
        device.pressBack()
        tap(By.text("All"))
        // Sources move to the top as their results arrive, so the list settles once the slowest ones are in.
        find(By.text("Sintel"))
        scrollTo(By.textStartsWith("The War of the Worlds"))
        settle()
        repeat(2) { list().fling(Direction.UP) }
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
}
