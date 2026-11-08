package app.katari.showcase.desktop.sync

import app.katari.showcase.desktop.DesktopScene
import app.katari.showcase.desktop.SceneContext
import app.katari.showcase.desktop.chooseProfile
import app.katari.showcase.desktop.driver.Desktop
import app.katari.showcase.desktop.driver.Selector
import app.katari.showcase.desktop.reader.openComicEpisode
import java.awt.event.KeyEvent

/**
 * Reads on in the episode the phone's hand-off scene has open, alongside that scene, which waits for the phone's reader
 * to offer the page this one stops at. The app stays open until the pipeline releases it, so it can send the progress.
 *
 * The desktop joined through sync without the seed's settings, so its reader reads in the default direction, where
 * Page Down turns to the next page in any direction; its guide to the mouse and keys stays up without taking the keys.
 * The window is wide enough for two pages side by side after the first, so each Page Down turns two: the first stops
 * at page 3, too near for the phone to offer it, and the second at page 5, which the phone offers.
 */
internal class ReadAheadScenes : DesktopScene {
    override fun run(scene: SceneContext) {
        chooseProfile("Personal")
        openComicEpisode()
        for (page in SPREAD_ENDS) {
            Desktop.press(KeyEvent.VK_PAGE_DOWN)
            Desktop.find(Selector.text("$page / 10"))
        }
        scene.awaitRelease()
    }

    private companion object {
        val SPREAD_ENDS = listOf(3, 5)
    }
}
