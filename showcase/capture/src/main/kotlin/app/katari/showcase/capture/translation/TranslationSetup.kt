package app.katari.showcase.capture.translation

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import app.katari.showcase.capture.HomeTab
import app.katari.showcase.capture.ShowcaseDevice.device
import app.katari.showcase.capture.ShowcaseDevice.fail
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.tap
import app.katari.showcase.capture.ShowcaseDevice.tapLast
import app.katari.showcase.capture.ShowcaseDevice.tapUntil
import app.katari.showcase.capture.openProfile
import app.katari.showcase.capture.openTab
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
        turnOnIncognito()
        openJapaneseEpisode()
        openTranslationBar()

        // The merged entry reads its pages in the language of its English root.
        tap(By.text(Pattern.compile(".* → .*")))
        tap(By.text("Page language"))
        find(By.clazz("android.widget.EditText")).text = "Japanese"
        // The search field holds the same word as the result, so the result is picked by its native name.
        tapUntil(By.text("日本語 · ja"), Until.hasObject(By.text("Download")))

        tap(By.text("Download"))
        find(By.text("Download models?"))
        tapLast(By.text("Download"))
        find(By.text("Japanese → English"), timeoutMs = MODEL_DOWNLOAD_TIMEOUT_MS)

        tap(By.desc("Show translations on the page"))
        // Offered once the page's text is recognized and the translator turns out not to be set up yet.
        tap(By.text("Set up"), timeoutMs = PAGE_TRANSLATION_TIMEOUT_MS)
        tap(By.text("Allow on-device translation"))
        closeTranslationBar()
    }

    /**
     * Reading here happens at the real date and must stay out of the showcase history. The app turns incognito off
     * whenever it launches, so it is turned on from More once the app is running.
     */
    private fun turnOnIncognito() {
        openTab(HomeTab.More)
        tap(By.text(INCOGNITO))
        // The switch reports no state, but turned on it shows a banner with the same title above every screen.
        val deadline = SystemClock.uptimeMillis() + INCOGNITO_TIMEOUT_MS
        while (device.findObjects(By.text(INCOGNITO)).size < 2) {
            if (SystemClock.uptimeMillis() > deadline) fail("Incognito mode never turned on")
            SystemClock.sleep(POLL_MS)
        }
    }

    private companion object {
        const val MODEL_DOWNLOAD_TIMEOUT_MS = 10 * 60_000L
        const val INCOGNITO = "Incognito mode"
        const val INCOGNITO_TIMEOUT_MS = 30_000L
        const val POLL_MS = 250L
    }
}
