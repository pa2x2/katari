package app.katari.showcase.capture.reader

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import app.katari.showcase.capture.ShowcaseDevice.capture
import app.katari.showcase.capture.ShowcaseDevice.tapToShow
import app.katari.showcase.capture.openProfile
import org.junit.Test
import org.junit.runner.RunWith

/** The comic reader on the first page of Pepper&Carrot's English episode 6, which Personal has yet to read. */
@RunWith(AndroidJUnit4::class)
class ComicScenes {
    @Test
    fun page() {
        openProfile("Personal")
        openComicEpisode()
        capture("comic-reader")
    }

    /** The reader's menus over the page, with Reading mode in the bottom bar. */
    @Test
    fun menus() {
        openProfile("Personal")
        openComicEpisode()
        tapToShow(0.5f, 0.5f, By.desc("Reading mode"))
        capture("comic-menus")
    }
}
