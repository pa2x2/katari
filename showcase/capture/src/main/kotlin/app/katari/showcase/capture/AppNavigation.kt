package app.katari.showcase.capture

import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import app.katari.showcase.capture.ShowcaseDevice.awaitGone
import app.katari.showcase.capture.ShowcaseDevice.device
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.restart
import app.katari.showcase.capture.ShowcaseDevice.scrollTo
import app.katari.showcase.capture.ShowcaseDevice.settle
import app.katari.showcase.capture.ShowcaseDevice.tap
import java.util.regex.Pattern

/** Restarts the app, which asks for a profile on every cold start once there is more than one, and picks [name]. */
internal fun openProfile(name: String) {
    restart()
    find(By.text("Select profile"))
    tap(By.descStartsWith(name))
    find(homeTab(HomeTab.Library))
}

/** The home destinations, named as the app's home navigation tags them. */
internal enum class HomeTab { Library, Updates, Browse, More, Translator }

/**
 * A home destination wherever it sits, in the dock or its menu, and whatever its label reads: the dock spells out only
 * the selected one's. A new selector every time, since UiAutomator's hasChild changes the one it is given.
 */
internal fun homeTab(tab: HomeTab): BySelector = By.res("home_tab_${tab.name.lowercase()}")

/**
 * Opens [tab], from the dock's menu if the profile's dock has no room for it. Tapping the tab already open does
 * something else, such as opening the library's filter sheet, so that one is left alone.
 */
internal fun openTab(tab: HomeTab) {
    find(By.res(ANY_HOME_TAB))
    val item = device.findObject(homeTab(tab))
    when {
        item == null -> {
            tap(By.res(HOME_DOCK_OVERFLOW))
            tap(homeTab(tab))
        }
        item.hasObject(By.selected(true)) -> Unit
        else -> tap(homeTab(tab))
    }
}

private val ANY_HOME_TAB: Pattern = Pattern.compile("home_tab_.+")
private const val HOME_DOCK_OVERFLOW = "home_tab_overflow"

/** Opens [title] from the library of the profile in use. */
internal fun openEntry(title: String) {
    openTab(HomeTab.Library)
    tap(By.text(title))
    settle()
}

/** Opens [section] of the settings of the profile in use. */
internal fun openSettings(section: String) {
    openTab(HomeTab.More)
    // Settings ends More's list, under the dock or below the screen, and taps scroll to it only after their wait.
    scrollTo(By.text("Settings"))
    tap(By.text("Settings"))
    // More lists some sections too, such as Data and storage, and stays on screen while Settings slides in over it.
    awaitGone(By.text("Download queue"))
    scrollTo(By.text(section))
    tap(By.text(section))
    settle()
}
