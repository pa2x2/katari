package app.katari.showcase.desktop.settings

import app.katari.showcase.desktop.DesktopScene
import app.katari.showcase.desktop.SceneContext
import app.katari.showcase.desktop.chooseProfile
import app.katari.showcase.desktop.driver.Desktop
import app.katari.showcase.desktop.driver.FileDialog
import app.katari.showcase.desktop.driver.Selector
import app.katari.showcase.desktop.openTab

/**
 * The docs' walkthrough of bringing a Mihon backup over (the `mihonBackup` argument), in the window: the file and the
 * app it was made by, what comes over, what the app settings leave behind, and restoring into a profile named after
 * Mihon. Nothing is restored.
 */
internal class BringOverScenes : DesktopScene {
    override fun run(scene: SceneContext) {
        chooseProfile("Personal")
        openTab("More")
        Desktop.click(Selector.text("Settings"))
        Desktop.scrollDownTo(Selector.text("Appearance"), DATA_AND_STORAGE)
        Desktop.click(DATA_AND_STORAGE)
        Desktop.click(Selector.text("Bring over from another app"))
        Desktop.click(Selector.text("Choose backup file"))
        FileDialog.choose(scene.argument("mihonBackup"))
        Desktop.find(Selector.text("Detected from the file"))
        scene.capture("desktop-bring-over-choose")

        Desktop.click(Selector.text("Convert"))
        Desktop.find(Selector.text("What comes over"))
        scene.capture("desktop-bring-over-report")

        Desktop.click(APP_SETTINGS)
        Desktop.find(Selector.text("Left behind"))
        // Up to the App settings row, with what it leaves behind below it.
        Desktop.scrollToTop(APP_SETTINGS)
        scene.capture("desktop-bring-over-left")

        Desktop.click(Selector.text("Restore"))
        // The profile in use is the one picked first; the row opens the choice.
        Desktop.click(Selector.text("Personal"))
        Desktop.click(Selector.text("New profile"))
        Desktop.find(Selector.text("New profile “Mihon”"))
        scene.capture("desktop-bring-over-restore")
    }

    private companion object {
        val DATA_AND_STORAGE = Selector.text("Data and storage")
        val APP_SETTINGS = Selector.text("App settings")
    }
}
