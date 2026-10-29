package eu.kanade.tachiyomi.extension

/** Graph entry point of the installer services and install activity, which can't be injected. */
interface ExtensionGraph {
    val extensionManager: ExtensionManager
}
