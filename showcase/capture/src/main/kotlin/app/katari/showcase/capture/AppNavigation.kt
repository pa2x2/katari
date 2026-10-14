package app.katari.showcase.capture

import androidx.test.uiautomator.By
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

/** Opens [title] from the library of the profile in use. */
internal fun openEntry(title: String) {
    tap(By.text("Library"))
    tap(By.text(title))
    settle()
}

/** Opens [section] of the settings of the profile in use. */
internal fun openSettings(section: String) {
    tap(By.text("More"))
    tap(By.text("Settings"))
    scrollTo(By.text(section)).click()
    settle()
}
