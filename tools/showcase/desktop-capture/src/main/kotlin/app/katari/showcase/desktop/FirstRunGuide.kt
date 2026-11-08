package app.katari.showcase.desktop

import app.katari.showcase.desktop.driver.Desktop
import app.katari.showcase.desktop.driver.FileDialog
import app.katari.showcase.desktop.driver.Selector

/** Moves the first-run guide on to the step titled [awaiting]. */
internal fun nextGuideStep(awaiting: String) {
    Desktop.click(Selector.text("Next"))
    Desktop.find(Selector.text(awaiting))
}

/** On the guide's Storage step, picks [folder] for downloads and backups. */
internal fun chooseStorageFolder(folder: String) {
    Desktop.click(Selector.text("Select a folder"))
    FileDialog.choose(folder)
    Desktop.find(Selector.textStartsWith("Selected folder:"))
}
