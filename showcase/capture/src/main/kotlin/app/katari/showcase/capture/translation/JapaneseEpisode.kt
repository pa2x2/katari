package app.katari.showcase.capture.translation

import androidx.test.uiautomator.By
import app.katari.showcase.capture.ShowcaseDevice.device
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.scrollTo
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
    scrollTo(By.text(EPISODE))
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

/**
 * How long the page's text may take to be recognized and translated. On a GitHub runner that takes 90 to 130 seconds,
 * most of it in Offline Translator, whose only build is for arm64 and runs translated on the x86_64 emulator.
 */
internal const val PAGE_TRANSLATION_TIMEOUT_MS = 5 * 60_000L

private const val EPISODE = "第6話"
