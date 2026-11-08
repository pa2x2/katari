package app.katari.showcase.desktop.reader

import app.katari.showcase.desktop.DesktopScene
import app.katari.showcase.desktop.SceneContext
import app.katari.showcase.desktop.chooseProfile
import app.katari.showcase.desktop.driver.Desktop
import app.katari.showcase.desktop.driver.Selector
import app.katari.showcase.desktop.openLibraryEntry

/**
 * Sherlock Holmes at the seeded position, paged left to right, which on the wide window shows two pages side by side.
 * The seed reads it scrolling, as the phone shows it.
 */
internal class BookScenes : DesktopScene {
    override fun run(scene: SceneContext) {
        chooseProfile("Personal")
        openLibraryEntry("The Adventures of Sherlock Holmes")
        Desktop.click(Selector.textMatches(Regex(""".*\d+% read""")))
        // The reader shows the read share once the source has sent the book and it is laid out.
        Desktop.find(Selector.textMatches(Regex("""\d+%""")), BOOK_OPEN_TIMEOUT_MS)
        Desktop.clickToShow(0.5f, 0.5f, READING_MODE)
        Desktop.click(READING_MODE)
        Desktop.click(Selector.text("Paged (left to right)"))
        Desktop.click(Selector.text("Apply"))
        Desktop.clickToHide(0.5f, 0.5f, READING_MODE)
        // Each page's number shows below it once the book is laid out in pages and the menus are gone.
        Desktop.find(Selector.textMatches(Regex("""\d+ / \d+""")))
        scene.capture("desktop-book")
    }

    private companion object {
        // The reader's menus show it, at the bottom.
        val READING_MODE = Selector.description("Reading mode")

        const val BOOK_OPEN_TIMEOUT_MS = 3 * 60_000L
    }
}
