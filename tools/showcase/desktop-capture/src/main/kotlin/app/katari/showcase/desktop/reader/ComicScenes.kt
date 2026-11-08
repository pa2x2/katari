package app.katari.showcase.desktop.reader

import app.katari.showcase.desktop.DesktopScene
import app.katari.showcase.desktop.SceneContext
import app.katari.showcase.desktop.chooseProfile
import app.katari.showcase.desktop.driver.Desktop
import app.katari.showcase.desktop.driver.Selector
import app.katari.showcase.desktop.openLibraryEntry

/** The right-click menu over the first page of Pepper&Carrot's English episode 6, as the phone's comic shots read it. */
internal class ComicScenes : DesktopScene {
    override fun run(scene: SceneContext) {
        chooseProfile("Personal")
        openLibraryEntry("Pepper&Carrot")
        // The entry opens with only the group of the chapter it would resume expanded: Personal's Japanese episode.
        Desktop.click(Selector.textMatches(Regex("""Pepper&Carrot \(EN\) · .*""")))
        Desktop.click(Selector.text("Episode 6: The Potion Contest"))
        Desktop.find(Selector.text("1 / 10"))
        // The page's actions are for a page that has loaded.
        scene.awaitStill()
        // Over the sky, clear of the speech bubbles.
        Desktop.rightClickAt(Desktop.mainWindow(), 0.62f, 0.2f)
        Desktop.find(Selector.text("Set as cover"))
        scene.capture("desktop-comic-menu")
    }
}
