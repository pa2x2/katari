package app.katari.showcase.capture.translation

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.katari.showcase.capture.ShowcaseDevice.awaitScreenChange
import app.katari.showcase.capture.ShowcaseDevice.capture
import app.katari.showcase.capture.ShowcaseDevice.settle
import app.katari.showcase.capture.ShowcaseDevice.tapAt
import app.katari.showcase.capture.openProfile
import org.junit.Test
import org.junit.runner.RunWith

/** The Japanese episode translated on the page, read on-device by text recognition and Offline Translator. */
@RunWith(AndroidJUnit4::class)
class TranslationScenes {
    @Test
    fun onPage() {
        openProfile("Personal")
        openJapaneseEpisode()
        capture("translation-before")

        translatePage()
        capture("translation-after")
    }

    /** One bubble's translation, with buttons that read the original or the translation aloud. */
    @Test
    fun popup() {
        openProfile("Personal")
        openJapaneseEpisode()
        translatePage()
        // The bubble beside Pepper on her flying bed, in the last panel.
        tapAt(0.815f, 0.687f)
        capture("translation-popup")
    }

    /**
     * Translation starts with the reader's translation controls and runs once per visit to the chapter. Its progress
     * isn't exposed to accessibility, so the page itself is watched until the translated text is drawn. Closing the
     * controls turns translation off, so they stay on screen below the page while the reader's bars are hidden.
     */
    private fun translatePage() {
        awaitScreenChange { openTranslationBar() }
        toggleReaderBars()
        settle()
    }
}
