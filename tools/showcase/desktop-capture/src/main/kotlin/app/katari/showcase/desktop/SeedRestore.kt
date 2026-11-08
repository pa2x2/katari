package app.katari.showcase.desktop

import app.katari.showcase.desktop.driver.Desktop
import app.katari.showcase.desktop.driver.FileDialog
import app.katari.showcase.desktop.driver.Selector

/**
 * Sets the app up as the scenes expect it, through its first-run guide: restores the seed backup (the `seed` argument),
 * keeps downloads in the `storage` folder, leaves sync for the scenes that show it, and trusts the showcase extension,
 * which the pipeline put where the app keeps installed extensions.
 */
internal class SeedRestore : DesktopScene {
    override fun run(scene: SceneContext) {
        Desktop.click(Selector.textStartsWith("I have a backup"))
        nextGuideStep(awaiting = "Restore backup")
        Desktop.click(Selector.text("Choose a backup file"))
        FileDialog.choose(scene.argument("seed"))
        Desktop.click(Selector.text("Restore"))
        Desktop.find(Selector.textStartsWith("Restore completed"), RESTORE_TIMEOUT_MS)
        nextGuideStep(awaiting = "Appearance")
        nextGuideStep(awaiting = "Storage")
        chooseStorageFolder(scene.argument("storage"))
        nextGuideStep(awaiting = "Use Katari on more devices?")
        Desktop.click(Selector.text("Skip"))
        Desktop.find(Selector.text("You're all set"))
        Desktop.click(Selector.text("Get started"))
        trustShowcaseExtension()
    }

    private companion object {
        // The restore runs in the background and takes seconds on a laptop; a CI runner is slower.
        const val RESTORE_TIMEOUT_MS = 3 * 60_000L
    }
}
