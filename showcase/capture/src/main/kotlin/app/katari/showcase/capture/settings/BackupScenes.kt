package app.katari.showcase.capture.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import app.katari.showcase.capture.ShowcaseDevice.capture
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.tap
import app.katari.showcase.capture.openProfile
import app.katari.showcase.capture.openSettings
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupScenes {
    @Test
    fun createBackup() {
        openProfile("Personal")
        openSettings("Data and storage")
        tap(By.text("Create backup"))
        find(By.textStartsWith("Include sensitive settings"))
        capture("create-backup")
    }
}
