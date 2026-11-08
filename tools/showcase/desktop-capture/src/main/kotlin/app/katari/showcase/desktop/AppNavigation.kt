package app.katari.showcase.desktop

import app.katari.showcase.desktop.driver.Desktop
import app.katari.showcase.desktop.driver.Selector
import javax.accessibility.AccessibleRole

/** Picks [name] on the profile screen the app opens with, as it has more than one profile. */
internal fun chooseProfile(name: String) {
    // A profile's button is named after it, followed by ", Active" for the one the app last used.
    Desktop.click(
        Selector("profile $name") {
            it.role == AccessibleRole.PUSH_BUTTON &&
                it.name?.substringBefore(", ") == name
        },
    )
    Desktop.find(Selector.tab("Library"))
}

internal fun openTab(name: String) = Desktop.click(Selector.tab(name))

/**
 * Opens [title] from the library's grid, which the app starts on. Picking the Library tab again there would open the
 * library's filter sheet instead.
 */
internal fun openLibraryEntry(title: String) = Desktop.click(Selector.text(title))

/** Opens [title] from [source]'s catalogue, which Browse lists on its Sources tab. */
internal fun openSourceEntry(source: String, title: String) {
    openTab("Browse")
    Desktop.click(Selector.tab("Sources"))
    Desktop.click(Selector.text(source))
    Desktop.click(Selector.text(title))
}
