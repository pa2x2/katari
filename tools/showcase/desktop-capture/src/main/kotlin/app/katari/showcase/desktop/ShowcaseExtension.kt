package app.katari.showcase.desktop

import app.katari.showcase.desktop.driver.Desktop
import app.katari.showcase.desktop.driver.Selector

/**
 * Trusts the showcase extension, which the pipeline put where the app keeps installed extensions; the app loads an
 * extension it didn't install itself only once trusted.
 */
internal fun trustShowcaseExtension() {
    openTab("Browse")
    Desktop.click(Selector.tab("Extensions"))
    Desktop.click(Selector.description("Trust"))
    // The dialog's button shows the word; the row's button above only has it as its description.
    Desktop.click(Selector.text("Trust"))
    Desktop.find(Selector.text("Installed"))
}
