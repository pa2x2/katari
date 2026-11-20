package app.katari.showcase.capture.reader

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import app.katari.showcase.capture.ShowcaseDevice.appId
import app.katari.showcase.capture.ShowcaseDevice.capture
import app.katari.showcase.capture.ShowcaseDevice.device
import app.katari.showcase.capture.ShowcaseDevice.fail
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.settle
import app.katari.showcase.capture.ShowcaseDevice.shell
import app.katari.showcase.capture.ShowcaseDevice.tap
import app.katari.showcase.capture.openEntry
import app.katari.showcase.capture.openProfile
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerScenes {
    /**
     * Sintel paused on the cliff at sunset. Neither a tap on the timeline nor a pause lands on the same frame twice, so
     * the open player is sent the benchmark build's paused-start extra, which seeks it there exactly and pauses it. The
     * extra also shows the controls and keeps them up until they are next used, as they hide by themselves sooner than
     * the still is taken.
     */
    @Test
    fun player() = onShot { capture("player") }

    /** The player's settings over the same frame, with another quality picked and waiting to be applied. */
    @Test
    fun settings() = onShot {
        tap(By.desc("Settings"))
        tap(By.text(OTHER_QUALITY))
        find(By.text("Apply"))
        capture("player-settings")
    }

    /** Runs [block] in the landscape player, paused on the shot with its controls showing. */
    private fun onShot(block: () -> Unit) {
        openProfile("Personal")
        openEntry("Sintel")
        // The episode row's other text changes with how much of it was watched.
        tap(By.textStartsWith("9/27/10"))
        find(By.pkg(appId).clazz("android.view.SurfaceView"))
        device.setOrientationLandscape()
        try {
            settle()
            // The player is not exported, which the pipeline's root shell may start anyway; it is open already, so it
            // takes the intent as a new one.
            val started = shell("am start -n $appId/$PLAYER --el paused_start_position_ms $SHOT_MS")
            if ("Error" in started) fail(started)
            // Nothing touches the screen from here on: a tap is a use of the controls, which ends the paused start's
            // hold on them, and one landing as they show hides them. The finished film shows Play too, until the seek
            // moves the label and buffers, which shows a spinner in its place; a CI runner's emulator takes over half a
            // minute to.
            find(By.text(SHOT_LABEL), BUFFERING_TIMEOUT_MS)
            find(By.desc("Play"), BUFFERING_TIMEOUT_MS)
            block()
        } finally {
            device.setOrientationNatural()
        }
    }

    private companion object {
        const val PLAYER = "eu.kanade.tachiyomi.ui.video.player.VideoPlayerActivity"
        const val SHOT_MS = 691_169L
        const val SHOT_LABEL = "11:31 / 14:48"

        // A version of the film the showcase extension offers besides the one playing, which it never fetches.
        const val OTHER_QUALITY = "480p"
        const val BUFFERING_TIMEOUT_MS = 2 * 60_000L
    }
}
