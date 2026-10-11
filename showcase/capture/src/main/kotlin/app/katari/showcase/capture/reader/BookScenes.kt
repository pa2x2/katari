package app.katari.showcase.capture.reader

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import app.katari.showcase.capture.ShowcaseDevice.capture
import app.katari.showcase.capture.ShowcaseDevice.device
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.settle
import app.katari.showcase.capture.ShowcaseDevice.tap
import app.katari.showcase.capture.ShowcaseDevice.tapAt
import app.katari.showcase.capture.openEntry
import app.katari.showcase.capture.openProfile
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

/** The book reader at the seeded position in Sherlock Holmes, in its reading themes. */
@RunWith(AndroidJUnit4::class)
class BookScenes {
    @Test
    fun themes() {
        openProfile("Personal")
        openEntry("The Adventures of Sherlock Holmes")
        tap(By.textContains("% read"))
        // The reader's footer shows the bare percentage; the entry screen's button that opened it says "% read".
        find(By.text(READER_PROGRESS))
        for (theme in listOf("Paper", "Dusk")) {
            chooseTheme(theme)
            capture("book-${theme.lowercase()}")
        }
        chooseTheme("App theme")
    }

    private fun chooseTheme(theme: String) {
        tapAt(0.5f, 0.5f)
        tap(By.desc("Settings"))
        tap(By.text("Appearance"))
        tap(By.text(theme))
        device.pressBack()
        settle()
        // Closing the settings leaves the reader's bars showing.
        if (device.hasObject(By.desc("Settings"))) tapAt(0.5f, 0.5f)
    }

    private companion object {
        val READER_PROGRESS: Pattern = Pattern.compile("""\d+%""")
    }
}
