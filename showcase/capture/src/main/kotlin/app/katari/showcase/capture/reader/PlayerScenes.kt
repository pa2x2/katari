package app.katari.showcase.capture.reader

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Until
import app.katari.showcase.capture.ShowcaseDevice.appId
import app.katari.showcase.capture.ShowcaseDevice.capture
import app.katari.showcase.capture.ShowcaseDevice.device
import app.katari.showcase.capture.ShowcaseDevice.fail
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.settle
import app.katari.showcase.capture.ShowcaseDevice.shell
import app.katari.showcase.capture.ShowcaseDevice.tap
import app.katari.showcase.capture.ShowcaseDevice.tapToShow
import app.katari.showcase.capture.openEntry
import app.katari.showcase.capture.openProfile
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerScenes {
    /**
     * Sintel paused on the cliff at sunset. Neither a tap on the timeline nor a pause lands on the same frame twice, so
     * the open player is sent the benchmark build's paused-start extra, which seeks it there exactly and pauses it.
     */
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
            // The player is not exported, which the pipeline's root shell may start anyway; it is open already, so it
            // takes the intent as a new one.
            val started = shell("am start -n $appId/$PLAYER --el paused_start_position_ms $SHOT_MS")
            if ("Error" in started) fail(started)
            // The finished film shows Play too, until the seek moves the label and buffers, which shows a spinner in
            // its place; a CI runner's emulator takes over half a minute to.
            awaitWithControls(By.text(SHOT_LABEL))
            awaitWithControls(By.desc("Play"))
            capture("player")
        } finally {
            device.setOrientationNatural()
        }
    }

    /** Waits for [selector] among the controls, which hide by themselves while the player buffers. */
    private fun awaitWithControls(selector: BySelector) {
        val deadline = SystemClock.uptimeMillis() + BUFFERING_TIMEOUT_MS
        while (true) {
            showControls()
            if (device.wait(Until.hasObject(selector), CONTROLS_WAIT_MS)) return
            if (SystemClock.uptimeMillis() > deadline) fail("The player never showed $selector")
        }
    }

    /** With a tap where none of the controls is: the middle of the screen is the play button. */
    private fun showControls() = tapToShow(EMPTY_SPOT_X, EMPTY_SPOT_Y, By.desc("Lock controls"))

    private companion object {
        const val PLAYER = "eu.kanade.tachiyomi.ui.video.player.VideoPlayerActivity"
        const val SHOT_MS = 691_169L
        const val SHOT_LABEL = "11:31 / 14:48"
        const val BUFFERING_TIMEOUT_MS = 2 * 60_000L
        const val CONTROLS_WAIT_MS = 2_000L

        // Between the title bar and the playback buttons of the landscape screen.
        const val EMPTY_SPOT_X = 0.25f
        const val EMPTY_SPOT_Y = 0.3f
    }
}
