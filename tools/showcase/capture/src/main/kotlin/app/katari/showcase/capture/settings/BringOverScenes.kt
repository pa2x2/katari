package app.katari.showcase.capture.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import app.katari.showcase.capture.ShowcaseDevice.appId
import app.katari.showcase.capture.ShowcaseDevice.argument
import app.katari.showcase.capture.ShowcaseDevice.capture
import app.katari.showcase.capture.ShowcaseDevice.dragUp
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.shell
import app.katari.showcase.capture.ShowcaseDevice.tap
import app.katari.showcase.capture.openProfile
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The docs' walkthrough of bringing a Mihon backup over (the `mihonBackup` argument): the file and the app it was made
 * by, what comes over, what the app settings leave behind, and restoring into a profile named after Mihon. Nothing is
 * restored.
 */
@RunWith(AndroidJUnit4::class)
class BringOverScenes {
    @Test
    fun bringOver() {
        openProfile("Personal")
        // Opened from outside the app, the backup goes to Restore backup, which sends another app's backup on to the
        // tool with the file chosen, as Choose backup file would have.
        shell(
            "am start -a android.intent.action.VIEW -t application/octet-stream -d ${argument("mihonBackup")} " +
                "--grant-read-uri-permission -n $appId/eu.kanade.tachiyomi.ui.main.MainActivity",
        )
        tap(By.text("Bring over from Mihon"))
        find(By.text("Detected from the file"))
        capture("bring-over-choose")

        tap(By.text("Convert"))
        find(By.text("What comes over"))
        capture("bring-over-report")

        tap(By.text("App settings"))
        find(By.text("Left behind"))
        // Up to the App settings row, with what it leaves behind below it.
        dragUp(LEFT_BEHIND_DRAG)
        capture("bring-over-left")

        tap(By.text("Restore"))
        // The profile in use is the one picked first; the row opens the choice.
        tap(By.text("Personal"))
        tap(By.text("New profile"))
        find(By.text("New profile “Mihon”"))
        capture("bring-over-restore")
    }

    private companion object {
        const val LEFT_BEHIND_DRAG = 0.3f
    }
}
