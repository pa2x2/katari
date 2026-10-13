package app.katari.showcase.capture.browse

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import app.katari.showcase.capture.ShowcaseDevice.capture
import app.katari.showcase.capture.ShowcaseDevice.device
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.scrollTo
import app.katari.showcase.capture.ShowcaseDevice.settle
import app.katari.showcase.capture.ShowcaseDevice.tap
import app.katari.showcase.capture.openProfile
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowseScenes {
    /** The showcase sources match genres, so one word finds comics in two languages, books, and a film. */
    @Test
    fun globalSearch() {
        openProfile("Personal")
        tap(By.text("Browse"))
        tap(By.text("Sources"))
        tap(By.desc("Global search"))
        find(By.clazz("android.widget.EditText")).text = "Fantasy"
        device.pressEnter()
        device.pressBack()
        tap(By.text("All"))
        // Sources move to the top as their results arrive, so the list settles once the slowest ones are in.
        find(By.text("Sintel"))
        scrollTo(By.textStartsWith("The War of the Worlds"))
        settle()
        repeat(2) { find(By.scrollable(true)).fling(Direction.UP) }
        capture("global-search")
    }

    @Test
    fun feed() {
        openProfile("Personal")
        tap(By.text("Browse"))
        tap(By.text("Feeds"))
        tap(By.text("Add feed"))
        tap(By.text("Project Gutenberg"))
        tap(By.text("Popular"))
        find(By.text("Treasure Island"))
        capture("feed")
    }
}
