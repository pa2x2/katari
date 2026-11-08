package app.katari.showcase.capture.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import app.katari.showcase.capture.ShowcaseDevice.awaitPromptly
import app.katari.showcase.capture.ShowcaseDevice.captureNow
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.tap
import app.katari.showcase.capture.openProfile
import app.katari.showcase.capture.openSettings
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Link a device, with a code and its QR code. The code counts down the two minutes it lives from the showcase moment,
 * where the pipeline sets the clock as the scene starts, and the sync server's clock stands. So the still is taken as
 * the count turns to [SHOWN_COUNT], at the same moment of every run.
 */
@RunWith(AndroidJUnit4::class)
class LinkCodeScenes {
    @Test
    fun linkCode() {
        openProfile("Personal")
        openSettings("Sync")
        tap(By.text("Link a device"))
        find(By.desc("QR code for linking a new device"))
        awaitPromptly(By.text("Expires in $COUNT_BEFORE."), COUNTDOWN_TIMEOUT_MS)
        awaitPromptly(By.text("Expires in $SHOWN_COUNT."))
        captureNow("sync-link-code")
    }

    private companion object {
        // A minute after the showcase moment leaves the scene that long to open the dialog.
        const val SHOWN_COUNT = "1:00"
        const val COUNT_BEFORE = "1:01"
        const val COUNTDOWN_TIMEOUT_MS = 2 * 60_000L
    }
}
