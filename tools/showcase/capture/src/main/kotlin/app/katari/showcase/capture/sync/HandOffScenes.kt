package app.katari.showcase.capture.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import app.katari.showcase.capture.ShowcaseDevice.capture
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.signalReady
import app.katari.showcase.capture.openProfile
import app.katari.showcase.capture.reader.openComicEpisode
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The reader on the first page of Pepper&Carrot's English episode 6 as word comes that the desktop read on: it offers
 * to go there. The desktop reads on in a scene alongside this one (scenes.ts), which starts once the reader is open.
 */
@RunWith(AndroidJUnit4::class)
class HandOffScenes {
    @Test
    fun handOff() {
        openProfile("Personal")
        openComicEpisode()
        signalReady()
        find(By.textStartsWith("Desktop is further"), OFFER_TIMEOUT_MS)
        capture("sync-handoff")
    }

    private companion object {
        // The desktop app starts, opens the episode and reads on before its progress arrives.
        const val OFFER_TIMEOUT_MS = 5 * 60_000L
    }
}
