package app.katari.showcase.capture.reader

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import app.katari.showcase.capture.ShowcaseDevice.capture
import app.katari.showcase.capture.ShowcaseDevice.device
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.settle
import app.katari.showcase.capture.ShowcaseDevice.tap
import app.katari.showcase.capture.ShowcaseDevice.tapAt
import app.katari.showcase.capture.openEntry
import app.katari.showcase.capture.openProfile
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerScenes {
    /** Sintel, paused on the same frame every run so the still only changes when the player does. */
    @Test
    fun player() {
        openProfile("Personal")
        openEntry("Sintel")
        // The episode row's other text changes with how much of it was watched.
        tap(By.textStartsWith("9/27/10"))
        find(By.desc("Lock controls"))
        device.setOrientationLandscape()
        try {
            settle()
            if (!device.hasObject(By.desc("Play"))) {
                tapAt(0.5f, 0.5f)
                tap(By.desc("Pause"))
            }
            // A point on the seek bar about a quarter of the way in, where Sintel meets the dragon.
            tapAt(0.26f, 0.38f)
            settle(extraMs = 3_000)
            if (!device.hasObject(By.desc("Play"))) tapAt(0.5f, 0.5f)
            capture("player")
        } finally {
            device.setOrientationNatural()
        }
    }
}
