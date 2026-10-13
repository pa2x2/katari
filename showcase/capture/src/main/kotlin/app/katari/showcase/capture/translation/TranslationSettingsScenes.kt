package app.katari.showcase.capture.translation

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import app.katari.showcase.capture.ShowcaseDevice.capture
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.tap
import app.katari.showcase.capture.openProfile
import app.katari.showcase.capture.openSettings
import org.junit.Test
import org.junit.runner.RunWith

/** Where translation is set up: the engine, text recognition for each language, and what chapters still need. */
@RunWith(AndroidJUnit4::class)
class TranslationSettingsScenes {
    @Test
    fun engines() {
        openProfile("Personal")
        openSettings("Translation")
        // The engine in use, named below the playground's languages, opens the list of engines.
        tap(By.text("Offline Translator"))
        find(By.text("LibreTranslate Server"))
        capture("translation-engines")
    }

    /** Japanese, which setup downloaded models for, beside the languages whose models would download on first use. */
    @Test
    fun recognitionLanguages() {
        openProfile("Personal")
        openSettings("Text recognition")
        tap(By.text("Languages"))
        find(By.text("Japanese manga"))
        capture("text-recognition")
    }

    /** The Family profile has set nothing up for translation, so the sheet lists what is missing. */
    @Test
    fun beforeTranslating() {
        openProfile("Family")
        tap(By.text("Pepper&Carrot"))
        find(By.text("Episode 7: The Wish")).longClick()
        tap(By.desc("Translate"))
        find(By.text("Before translating"))
        capture("before-translating")
    }
}
