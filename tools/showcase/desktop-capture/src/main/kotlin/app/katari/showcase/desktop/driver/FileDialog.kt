package app.katari.showcase.desktop.driver

import javax.accessibility.AccessibleRole
import javax.swing.JDialog

/**
 * The Swing file dialog the app falls back to where no desktop portal answers, as on the pipeline's private session
 * bus. It names its one text field "File Name:" or "Folder name:", by what it picks.
 */
internal object FileDialog {
    private val nameField = Selector("the file dialog's name field") {
        it.window is JDialog && it.role == AccessibleRole.TEXT
    }
    private val openButton = Selector("the file dialog's Open") {
        it.window is JDialog && it.role == AccessibleRole.PUSH_BUTTON && it.name == "Open"
    }

    /** Picks [path], which may be a file or a folder, by typing it in and confirming. */
    fun choose(path: String) {
        Desktop.enterText(nameField, path)
        Desktop.click(openButton)
        Desktop.awaitGone(nameField)
    }
}
