package app.katari.showcase.capture

import androidx.test.uiautomator.By
import app.katari.showcase.capture.ShowcaseDevice.awaitGone
import app.katari.showcase.capture.ShowcaseDevice.device
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.restart
import app.katari.showcase.capture.ShowcaseDevice.scrollTo
import app.katari.showcase.capture.ShowcaseDevice.settle
import app.katari.showcase.capture.ShowcaseDevice.tap

/** Restarts the app, which asks for a profile on every cold start once there is more than one, and picks [name]. */
internal fun openProfile(name: String) {
    restart()
    find(By.text("Select profile"))
    tap(By.descStartsWith(name))
    find(By.text("Library"))
}

/**
 * Opens the home tab labelled [label]. The dock spells out only the selected tab's label, and the rest show it as their
 * icon's description, which the selected one carries too. Tapping the tab already open does something else, such as
 * opening the library's filter sheet, so that one is left alone.
 */
internal fun openTab(label: String) {
    find(By.desc(label))
    // hasChild limits the selector it is given to the depth below its parent, so that one is not reused.
    if (!device.hasObject(By.selected(true).hasChild(By.desc(label)))) tap(By.desc(label))
}

/** Opens [title] from the library of the profile in use. */
internal fun openEntry(title: String) {
    openTab("Library")
    tap(By.text(title))
    settle()
}

/** Opens [section] of the settings of the profile in use. */
internal fun openSettings(section: String) {
    openTab("More")
    // The dock floats over the end of More's list, where Settings is.
    scrollTo(By.text("Settings"))
    tap(By.text("Settings"))
    // More lists some sections too, such as Data and storage, and stays on screen while Settings slides in over it.
    awaitGone(By.text("Download queue"))
    scrollTo(By.text(section))
    tap(By.text(section))
    settle()
}
