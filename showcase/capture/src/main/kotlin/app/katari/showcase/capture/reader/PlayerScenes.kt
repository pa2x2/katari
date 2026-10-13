package app.katari.showcase.capture.reader

import android.os.SystemClock
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
import java.util.regex.Pattern

@RunWith(AndroidJUnit4::class)
class PlayerScenes {
    /**
     * Sintel paused on the cliff at sunset. A tap on the timeline can only aim within a second or so, and a pause lands
     * wherever playback has got to, so the shot is aimed only once the film is paused: from then on a tap seeks exactly
     * where it lands and the player stays there, so every run taps the same spots and shows the same frame.
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
            // The seed finished the film, so it opens on its last frame, where a seek starts it playing a moment
            // later; it can still show Play when the first pause looks, so every tap checks it stayed paused.
            tapTimeline()
            var attempts = 0
            while (true) {
                check(attempts++ < SEEK_ATTEMPTS) { "The player never stopped at $TARGET_SECONDS seconds" }
                pause()
                tapTimeline()
                settle(extraMs = 3_000)
                if (!device.hasObject(By.desc("Play"))) continue
                val shown = shownSeconds()
                if (shown == TARGET_SECONDS) break
                tapX += (TARGET_SECONDS - shown) * (TIMELINE_END - TIMELINE_START) / DURATION_SECONDS
            }
            showControls()
            capture("player")
        } finally {
            device.setOrientationNatural()
        }
    }

    // Aimed at the middle of the second, which leaves the most room for where the tap lands.
    private var tapX = TIMELINE_START + (TIMELINE_END - TIMELINE_START) * (TARGET_SECONDS + 0.5f) / DURATION_SECONDS

    /** Taps the timeline, which runs along the bottom of the landscape screen. */
    private fun tapTimeline() {
        showControls()
        tapAt(tapX, TIMELINE_Y)
        settle()
    }

    private fun shownSeconds(): Int {
        showControls()
        val (minutes, seconds) = find(By.text(TIME_LABEL)).text.substringBefore(" ").split(":")
        return minutes.toInt() * 60 + seconds.toInt()
    }

    /** Playing, the controls hide after a moment, so a tap can find them gone and only bring them back. */
    private fun pause() {
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (!device.hasObject(By.desc("Play"))) {
            check(SystemClock.uptimeMillis() < deadline) { "The player never paused" }
            val pause = device.findObject(By.desc("Pause"))
            if (pause != null) {
                runCatching { pause.click() }
            } else {
                tapAt(0.5f, 0.5f)
            }
            SystemClock.sleep(500)
        }
    }

    private fun showControls() {
        if (!device.hasObject(By.desc("Lock controls"))) {
            tapAt(0.5f, 0.5f)
            find(By.desc("Lock controls"))
        }
    }

    private companion object {
        const val SEEK_ATTEMPTS = 6
        const val TARGET_SECONDS = 691
        const val DURATION_SECONDS = 888f

        // Where the timeline's track starts, ends, and runs, as fractions of the landscape screen.
        const val TIMELINE_START = 0.0825f
        const val TIMELINE_END = 0.918f
        const val TIMELINE_Y = 0.854f

        val TIME_LABEL: Pattern = Pattern.compile("""\d+:\d\d / 14:48""")
    }
}
