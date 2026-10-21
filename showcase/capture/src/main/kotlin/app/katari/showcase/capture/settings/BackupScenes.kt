package app.katari.showcase.capture.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import app.katari.showcase.capture.ShowcaseDevice.awaitGone
import app.katari.showcase.capture.ShowcaseDevice.capture
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.list
import app.katari.showcase.capture.ShowcaseDevice.tap
import app.katari.showcase.capture.openProfile
import app.katari.showcase.capture.openSettings
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupScenes {
    /** The guide walks through the settings group and its sensitive settings box, which end the screen. */
    @Test
    fun createBackup() {
        openProfile("Personal")
        openSettings("Data and storage")
        tap(By.text("Create backup"))
        // Data and storage, whose list would be the one scrolled, stays on screen while this one slides in over it.
        awaitGone(By.text("Restore backup"))
        val options = list()
        // Released slower than a fling, the list stops at its end instead of bouncing differently each run.
        do {
            val more = options.scroll(Direction.DOWN, 1f, SCROLL_PX_PER_SECOND)
        } while (more)
        find(By.textStartsWith("Include sensitive settings"))
        capture("create-backup")
    }

    private companion object {
        const val SCROLL_PX_PER_SECOND = 100
    }
}
