package app.katari.showcase.capture.reader

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import app.katari.showcase.capture.ShowcaseDevice.awaitStill
import app.katari.showcase.capture.ShowcaseDevice.capture
import app.katari.showcase.capture.ShowcaseDevice.device
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.pressAndDrag
import app.katari.showcase.capture.ShowcaseDevice.settle
import app.katari.showcase.capture.ShowcaseDevice.tap
import app.katari.showcase.capture.ShowcaseDevice.tapToHide
import app.katari.showcase.capture.ShowcaseDevice.tapToShow
import app.katari.showcase.capture.openEntry
import app.katari.showcase.capture.openProfile
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

/** The book reader at the seeded position in Sherlock Holmes: its reading themes, its settings and a selection. */
@RunWith(AndroidJUnit4::class)
class BookScenes {
    @Test
    fun themes() {
        openBook()
        for (theme in listOf("Paper", "Dusk")) {
            chooseTheme(theme)
            capture("book-${theme.lowercase()}")
        }
        chooseTheme("App theme")
    }

    /** The settings' Appearance tab, with the themes and fonts, over the page. */
    @Test
    fun appearance() {
        openBook()
        tapToShow(0.5f, 0.5f, READER_BARS)
        tap(READER_BARS)
        tap(By.text("Appearance"))
        find(By.text("Paper"))
        capture("book-appearance")
    }

    /** Holmes's line that the signal has come, selected whole, with the actions it offers. */
    @Test
    fun selection() {
        openBook()
        awaitStill()
        // The reader lays out each paragraph as a text of its own, and this one runs over two lines: the press lands in
        // its first word, past the indent, and the drag ends past the last word of its second line.
        val paragraph = find(By.textStartsWith(SELECTED_PARAGRAPH)).visibleBounds
        pressAndDrag(
            startX = paragraph.left + paragraph.width() / 10,
            startY = paragraph.top + paragraph.height() / 4,
            endX = paragraph.right - 1,
            endY = paragraph.bottom - paragraph.height() / 4,
        )
        find(By.text("Listen"))
        capture("book-selection")
    }

    private fun openBook() {
        openProfile("Personal")
        openEntry("The Adventures of Sherlock Holmes")
        tap(By.textContains("% read"))
        // The reader's footer shows the bare percentage; the entry screen's button that opened it says "% read".
        find(By.text(READER_PROGRESS))
    }

    private fun chooseTheme(theme: String) {
        tapToShow(0.5f, 0.5f, READER_BARS)
        tap(READER_BARS)
        tap(By.text("Appearance"))
        tap(By.text(theme))
        device.pressBack()
        settle()
        // Closing the settings leaves the reader's bars showing.
        tapToHide(0.5f, 0.5f, READER_BARS)
    }

    private companion object {
        val READER_PROGRESS: Pattern = Pattern.compile("""\d+%""")

        // The reader's settings button, on screen only while its bars are.
        val READER_BARS: BySelector = By.desc("Settings")

        const val SELECTED_PARAGRAPH = "“That is our signal,”"
    }
}
