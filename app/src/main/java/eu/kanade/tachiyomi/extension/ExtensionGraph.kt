package eu.kanade.tachiyomi.extension

import eu.kanade.tachiyomi.extension.file.InstalledFileExtensionIcons
import eu.kanade.tachiyomi.extension.util.ExtensionLoader

/** Graph entry points of the installer services, install activity and icon fetchers, which can't be injected. */
interface ExtensionGraph {
    val extensionManager: ExtensionManager

    val extensionLoader: ExtensionLoader

    val installedFileExtensionIcons: InstalledFileExtensionIcons
}
