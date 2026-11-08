package eu.kanade.tachiyomi.extension.kind

import eu.kanade.tachiyomi.extension.model.Extension

/** Which extensions the platform can run, by the family each belongs to and the library version it was built with. */
interface ExtensionCompatibility {
    /** What the extensions screen's compatibility dialog lists; empty where the platform runs no extensions. */
    val families: List<ExtensionFamilySupport>

    /**
     * Stores list extensions the platform can't run too: torrent extensions and ones built against a library version it
     * doesn't load. They're shown, so the user sees why, but never installed or offered as updates.
     */
    fun isInstallable(listing: Extension.Available): Boolean

    /** Why the platform can't run [extension], when that's its library's version. */
    fun unsupportedSdk(extension: Extension): UnsupportedSdk?
}
