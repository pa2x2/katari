package app.katari.showcase.capture.translation

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import app.katari.showcase.capture.ShowcaseDevice.awaitGone
import app.katari.showcase.capture.ShowcaseDevice.capture
import app.katari.showcase.capture.ShowcaseDevice.settle
import app.katari.showcase.capture.ShowcaseDevice.tapAt
import app.katari.showcase.capture.openProfile
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

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
     * Translation starts with the reader's translation controls and runs once per visit to the chapter. Until the
     * page's bubbles are recognized, translated and drawn, the controls say what they are doing. Closing the controls
     * turns translation off, so they stay on screen below the page while the reader's bars are hidden.
     */
    private fun translatePage() {
        openTranslationBar()
        toggleReaderBars()
        // The controls may not have started on the page yet when first looked at.
        repeat(2) {
            awaitGone(By.text(WORKING), TRANSLATION_TIMEOUT_MS)
            settle(extraMs = 3_000)
        }
    }

    private companion object {
        const val TRANSLATION_TIMEOUT_MS = 120_000L
        val WORKING: Pattern = Pattern.compile("Waiting for the page to load|Recognizing text…|Translating…")
    }
}
