package app.katari.showcase.capture

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import app.katari.showcase.capture.ShowcaseDevice.appId
import app.katari.showcase.capture.ShowcaseDevice.argument
import app.katari.showcase.capture.ShowcaseDevice.device
import app.katari.showcase.capture.ShowcaseDevice.fail
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.launch
import app.katari.showcase.capture.ShowcaseDevice.shell
import app.katari.showcase.capture.ShowcaseDevice.tap
import app.katari.showcase.capture.ShowcaseDevice.tapUntil
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
        tapUntil(By.desc("Browse"), Until.hasObject(By.text("Extensions")))
        tapUntil(By.text("Extensions"), Until.hasObject(By.desc("Trust")))
        tapUntil(By.desc("Trust"), Until.hasObject(By.text("Trust")))
        tap(By.text("Trust"))

        shell(
            "am start -a android.intent.action.VIEW -t application/octet-stream -d ${argument("seedUri")} " +
                "--grant-read-uri-permission -n $appId/eu.kanade.tachiyomi.ui.main.MainActivity",
        )
        startRestore()
        awaitRestoreCompleted()
        clearNotifications()
    }

    /**
     * The screen reads the backup before its button works, and a tap before then does nothing. Starting the restore
     * closes the screen, which tells a tap that took; the app keeps a restore already running.
     */
    private fun startRestore() {
        find(By.text(RESTORE_SCREEN_TITLE))
        tapUntil(By.text("Restore"), Until.gone(By.text(RESTORE_SCREEN_TITLE)), START_TIMEOUT_MS)
    }

    /**
     * The restore makes each profile active while it fills it in, so the first profile's library shows up while the
     * others are still being written. Only the notification it posts at the very end marks it finished.
     */
    private fun awaitRestoreCompleted() {
        val deadline = SystemClock.uptimeMillis() + RESTORE_TIMEOUT_MS
        while (!shell("dumpsys notification --noredact").contains(RESTORE_COMPLETED)) {
            if (SystemClock.uptimeMillis() > deadline) fail("The restore never completed")
            SystemClock.sleep(1_000)
        }
    }

    /**
     * The restore's notifications set off the system's notification cooldown notice, and demo mode doesn't hide
     * notification icons from the status bar of every screenshot that follows.
     */
    private fun clearNotifications() {
        device.openNotification()
        tap(By.text("Clear all"))
        // The shade closes once it is empty, so whether it worked is read from the notification service instead.
        val deadline = SystemClock.uptimeMillis() + CLEAR_TIMEOUT_MS
        while (shell("dumpsys notification --noredact").contains(RESTORE_COMPLETED)) {
            if (SystemClock.uptimeMillis() > deadline) fail("The notifications could not be cleared")
            SystemClock.sleep(500)
        }
        // Back would reach the app if the shade had already closed.
        shell("cmd statusbar collapse")
    }

    private companion object {
        const val RESTORE_SCREEN_TITLE = "Restore backup"
        const val START_TIMEOUT_MS = 60_000L
        const val RESTORE_COMPLETED = "Restore completed"
        const val RESTORE_TIMEOUT_MS = 180_000L
        const val CLEAR_TIMEOUT_MS = 10_000L
    }
}
