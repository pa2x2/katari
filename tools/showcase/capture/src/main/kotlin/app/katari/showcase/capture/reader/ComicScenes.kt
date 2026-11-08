package app.katari.showcase.capture.reader

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import app.katari.showcase.capture.ShowcaseDevice.capture
import app.katari.showcase.capture.ShowcaseDevice.device
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.scrollTo
import app.katari.showcase.capture.ShowcaseDevice.settle
import app.katari.showcase.capture.ShowcaseDevice.tap
import app.katari.showcase.capture.ShowcaseDevice.tapToShow
import app.katari.showcase.capture.openEntry
import app.katari.showcase.capture.openProfile
import org.junit.Test
import org.junit.runner.RunWith

/** The comic reader on the first page of Pepper&Carrot's English episode 6, which Personal has yet to read. */
@RunWith(AndroidJUnit4::class)
class ComicScenes {
    @Test
    fun page() {
        openEpisode()
        capture("comic-reader")
    }

    /** The reader's menus over the page, with Reading mode in the bottom bar. */
    @Test
    fun menus() {
        openEpisode()
        tapToShow(0.5f, 0.5f, By.desc("Reading mode"))
        capture("comic-menus")
    }

    private fun openEpisode() {
        openProfile("Personal")
        openEntry("Pepper&Carrot")
        // The entry opens with the group of the chapter it would resume expanded: Personal's Japanese episode, until a
        // scene reads the English one. A tap on the English group's header toggles it, and the header doesn't say which
        // way, so it is tapped only while none of the group's episodes are listed under it.
        find(ENGLISH_GROUP)
        if (!device.hasObject(ENGLISH_EPISODE)) tap(ENGLISH_GROUP)
        scrollTo(By.text(EPISODE))
        tap(By.text(EPISODE))
        find(By.text("1 / 10"))
        // The page loads after the reader shows its counter.
        settle(extraMs = 3_000)
    }

    private companion object {
        const val EPISODE = "Episode 6: The Potion Contest"

        // The English member's header sums it up as "Pepper&Carrot (EN) · 8 chapters"; the entry's line of sources,
        // which opens the merge's settings, goes on after the name with a comma.
        val ENGLISH_GROUP: BySelector = By.textContains("Pepper&Carrot (EN) · ")
        val ENGLISH_EPISODE: BySelector = By.textStartsWith("Episode ")
    }
}
