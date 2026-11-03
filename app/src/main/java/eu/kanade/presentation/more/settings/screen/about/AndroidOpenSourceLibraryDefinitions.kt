package eu.kanade.presentation.more.settings.screen.about

import android.app.Application
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.R

/** The definitions AboutLibraries' Android plugin generates as the app's raw resource. */
@Inject
class AndroidOpenSourceLibraryDefinitions(private val application: Application) : OpenSourceLibraryDefinitions {
    override fun readJson(): String =
        application.resources.openRawResource(R.raw.aboutlibraries).bufferedReader().use { it.readText() }
}
