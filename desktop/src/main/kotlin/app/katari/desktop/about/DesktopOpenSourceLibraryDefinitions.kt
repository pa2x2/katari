package app.katari.desktop.about

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.about.OpenSourceLibraryDefinitions

/** The definitions AboutLibraries' plugin exports from the desktop app's dependencies into its resources. */
@Inject
class DesktopOpenSourceLibraryDefinitions : OpenSourceLibraryDefinitions {
    override fun readJson(): String {
        val stream = checkNotNull(javaClass.getResourceAsStream(RESOURCE)) { "$RESOURCE is not in the build" }
        return stream.bufferedReader().use { it.readText() }
    }

    private companion object {
        const val RESOURCE = "/aboutlibraries.json"
    }
}
