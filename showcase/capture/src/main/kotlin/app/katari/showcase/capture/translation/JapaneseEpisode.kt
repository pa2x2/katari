package app.katari.showcase.capture.translation

import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import app.katari.showcase.capture.ShowcaseDevice.device
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.settle
import app.katari.showcase.capture.ShowcaseDevice.tap
import app.katari.showcase.capture.ShowcaseDevice.tapAt
import app.katari.showcase.capture.openEntry

/**
 * Opens the Japanese episode the seed left on its second page, whose speech bubbles the translation scenes read. It
 * is listed below the English episodes of the merged Pepper&Carrot entry.
 */
internal fun openJapaneseEpisode() {
    openEntry("Pepper&Carrot")
    find(By.scrollable(true)).scrollUntil(Direction.DOWN, Until.findObject(By.text(EPISODE)))
    tap(By.text(EPISODE))
    find(By.textContains("/ 10"))
    settle(extraMs = 3_000)
}

/** Shows the reader's bars and its translation controls. */
internal fun openTranslationBar() {
    toggleReaderBars()
    tap(By.desc("Translate text"))
}

/** Closes the translation controls and the reader's bars, leaving only the page. */
internal fun closeTranslationBar() {
    // The reader's own Close, which leaves the chapter, comes first.
    device.findObjects(By.desc("Close")).last().click()
    settle()
    if (device.hasObject(By.desc("Translate text"))) toggleReaderBars()
}

// A spot in the middle column of the second page that no speech bubble covers, so the tap reaches the reader.
internal fun toggleReaderBars() = tapAt(0.5f, 0.3f)

private const val EPISODE = "第6話"
