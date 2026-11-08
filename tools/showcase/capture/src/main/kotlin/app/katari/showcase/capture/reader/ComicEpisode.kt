package app.katari.showcase.capture.reader

import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import app.katari.showcase.capture.ShowcaseDevice.device
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.scrollTo
import app.katari.showcase.capture.ShowcaseDevice.settle
import app.katari.showcase.capture.ShowcaseDevice.tap
import app.katari.showcase.capture.openEntry

/** Opens the first page of Pepper&Carrot's English episode 6, which the comic shots read on both apps. */
internal fun openComicEpisode() {
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

private const val EPISODE = "Episode 6: The Potion Contest"

// The English member's header sums it up as "Pepper&Carrot (EN) · 8 chapters"; the entry's line of sources, which
// opens the merge's settings, goes on after the name with a comma.
private val ENGLISH_GROUP: BySelector = By.textContains("Pepper&Carrot (EN) · ")
private val ENGLISH_EPISODE: BySelector = By.textStartsWith("Episode ")
