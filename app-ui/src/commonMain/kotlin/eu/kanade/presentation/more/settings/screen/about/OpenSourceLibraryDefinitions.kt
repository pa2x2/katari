package eu.kanade.presentation.more.settings.screen.about

/** AboutLibraries' definitions of the libraries the app is built with, which each platform's build generates. */
fun interface OpenSourceLibraryDefinitions {
    /** The definitions as AboutLibraries' JSON. Reads a file, so it is called off the main thread. */
    fun readJson(): String
}
