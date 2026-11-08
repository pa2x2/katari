package app.katari.showcase.desktop.reader

import app.katari.showcase.desktop.DesktopScene
import app.katari.showcase.desktop.SceneContext
import app.katari.showcase.desktop.chooseProfile
import app.katari.showcase.desktop.driver.Desktop
import app.katari.showcase.desktop.driver.Selector
import app.katari.showcase.desktop.openSourceEntry
import java.awt.event.KeyEvent

/**
 * Caminandes' Gran Dillama paused on Koro tumbling by the electric fence: with the list of the player's keys, then in
 * the picture-in-picture window.
 */
internal class PlayerScenes : DesktopScene {
    override fun run(scene: SceneContext) {
        chooseProfile("Personal")
        openSourceEntry("Blender Studio", "Caminandes")
        Desktop.click(Selector.text("Gran Dillama"))
        // The player starts playing once the episode has buffered.
        Desktop.click(Selector.description("Pause"), STREAM_START_TIMEOUT_MS)
        Desktop.find(Selector.description("Play"))
        // Neither a pause nor a click on the timeline lands on the same frame twice; set as an assistive tool sets it,
        // the timeline seeks to exactly this share of the episode, where mpv shows the frame at that moment.
        Desktop.setProgress(Selector.description("Playback position"), SHOT_POSITION)
        Desktop.find(Selector.text(SHOT_LABEL), STREAM_START_TIMEOUT_MS)

        Desktop.press(KeyEvent.VK_SHIFT, KeyEvent.VK_SLASH)
        // The list starts with the keys every screen shares; the player's own follow.
        Desktop.scrollToTop(Selector.text("Current screen"))
        scene.capture("desktop-keys")

        Desktop.pressEscape()
        Desktop.awaitGone(Selector.text("Keyboard shortcuts"))
        Desktop.press(KeyEvent.VK_I)
        scene.capture("desktop-pip", Desktop.otherWindow())
    }

    private companion object {
        const val SHOT_POSITION = 0.45f
        const val SHOT_LABEL = "1:05 / 2:26"

        // The content server streams the episode from Wikimedia Commons as the player reads it.
        const val STREAM_START_TIMEOUT_MS = 2 * 60_000L
    }
}
