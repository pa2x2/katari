package app.katari.showcase.capture.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import app.katari.showcase.capture.ShowcaseDevice.capture
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.openProfile
import app.katari.showcase.capture.openSettings
import org.junit.Test
import org.junit.runner.RunWith

/** The Sync page once the desktop has joined: the account, both devices and the way to link another. */
@RunWith(AndroidJUnit4::class)
class SyncPageScenes {
    @Test
    fun syncPage() {
        openProfile("Personal")
        openSettings("Sync")
        find(By.text("Desktop"))
        // The page syncs as it opens.
        find(By.textStartsWith("Up to date"))
        capture("sync-page")
    }
}
