package app.katari.showcase.desktop.reader

import app.katari.showcase.desktop.driver.Desktop
import app.katari.showcase.desktop.driver.Selector
import app.katari.showcase.desktop.openLibraryEntry

/** Opens the first page of Pepper&Carrot's English episode 6, which the comic shots read on both apps. */
internal fun openComicEpisode() {
    openLibraryEntry("Pepper&Carrot")
    // The entry opens with the group of the chapter it would resume expanded: Personal's Japanese episode, until a
    // scene reads the English one. A click on the English group's header toggles it, and the header doesn't say which
    // way, so it is clicked only while none of the group's episodes are listed under it.
    Desktop.find(ENGLISH_GROUP)
    if (!Desktop.has(ENGLISH_EPISODE)) Desktop.click(ENGLISH_GROUP)
    Desktop.click(Selector.text("Episode 6: The Potion Contest"))
    Desktop.find(Selector.text("1 / 10"))
}

// The English member's header sums it up as "Pepper&Carrot (EN) · 8 chapters"; the entry's line of sources goes on
// after the name with a comma.
private val ENGLISH_GROUP = Selector.textMatches(Regex("""Pepper&Carrot \(EN\) · .*"""))
private val ENGLISH_EPISODE = Selector.textStartsWith("Episode ")
