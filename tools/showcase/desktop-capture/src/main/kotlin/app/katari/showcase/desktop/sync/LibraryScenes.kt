package app.katari.showcase.desktop.sync

import app.katari.showcase.desktop.DesktopScene
import app.katari.showcase.desktop.SceneContext
import app.katari.showcase.desktop.chooseProfile
import app.katari.showcase.desktop.driver.Desktop
import app.katari.showcase.desktop.driver.Selector

/** Personal's library as the desktop took it in from the phone through sync: comics, books and films. */
internal class LibraryScenes : DesktopScene {
    override fun run(scene: SceneContext) {
        chooseProfile("Personal")
        // The library's last title, by the order it lists them in.
        Desktop.find(Selector.text("羅生門"))
        // Covers fade in as they load from the content server.
        scene.awaitStill()
        scene.capture("sync-desktop")
    }
}
