package app.katari.showcase.capture

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import app.katari.showcase.capture.ShowcaseDevice.appId
import app.katari.showcase.capture.ShowcaseDevice.argument
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.launch
import app.katari.showcase.capture.ShowcaseDevice.shell
import app.katari.showcase.capture.ShowcaseDevice.tap
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Brings a fresh install to the seeded state through the app's own screens, since extension trust is app state that
 * backups leave out. Expects the seed backup at the content uri in the `seedUri` argument, readable by the app.
 */
@RunWith(AndroidJUnit4::class)
class SeedRestore {
    @Test
    fun restoreSeed() {
        launch()
        tap(By.text("Browse"))
        tap(By.text("Extensions"))
        tap(By.desc("Trust"))
        tap(By.text("Trust"))

        shell(
            "am start -a android.intent.action.VIEW -t application/octet-stream -d ${argument("seedUri")} " +
                "--grant-read-uri-permission -n $appId/eu.kanade.tachiyomi.ui.main.MainActivity",
        )
        tap(By.text("Restore"))
        // The library fills in once the restore finishes.
        find(By.text("Sintel"), timeoutMs = 120_000)
    }
}
