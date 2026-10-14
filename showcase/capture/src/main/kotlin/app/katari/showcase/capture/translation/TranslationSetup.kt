package app.katari.showcase.capture.translation

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import app.katari.showcase.capture.ShowcaseDevice.device
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.tap
import app.katari.showcase.capture.openProfile
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

/**
 * Readies on-page translation of the Japanese episode once, before the scenes run: reads its pages as Japanese,
 * downloads the text recognition models, and accepts the Offline Translator disclosure, which backups leave out. The models come over HTTPS, so
 * this runs while the device clock is still real; the pinned showcase date fails certificate checks.
 */
@RunWith(AndroidJUnit4::class)
class TranslationSetup {
    @Test
    fun prepare() {
        openProfile("Personal")
        openJapaneseEpisode()
        openTranslationBar()

        // The merged entry reads its pages in the language of its English root.
        tap(By.text(Pattern.compile(".* → .*")))
        tap(By.text("Page language"))
        find(By.clazz("android.widget.EditText")).text = "Japanese"
        // The search field holds the same word as the result, so the result is picked by its native name.
        tap(By.text("日本語 · ja"))

        tap(By.text("Download"))
        find(By.text("Download models?"))
        device.findObjects(By.text("Download")).last().click()
        find(By.text("Japanese → English"), timeoutMs = MODEL_DOWNLOAD_TIMEOUT_MS)

        tap(By.desc("Show translations on the page"))
        // Offered once the page's text is recognized and the translator turns out not to be set up yet.
        tap(By.text("Set up"), timeoutMs = PAGE_TRANSLATION_TIMEOUT_MS)
        tap(By.text("Allow on-device translation"))
        closeTranslationBar()
    }

    private companion object {
        const val MODEL_DOWNLOAD_TIMEOUT_MS = 10 * 60_000L
    }
}
