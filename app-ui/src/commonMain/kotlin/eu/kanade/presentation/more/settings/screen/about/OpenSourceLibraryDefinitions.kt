package eu.kanade.presentation.more.settings.screen.about

import mihon.core.common.platform.PlatformFacility

/** AboutLibraries' definitions of the libraries the app is built with, which each platform's build generates. */
@PlatformFacility
fun interface OpenSourceLibraryDefinitions {
    /** The definitions as AboutLibraries' JSON. Reads a file, so it is called off the main thread. */
    fun readJson(): String
}
