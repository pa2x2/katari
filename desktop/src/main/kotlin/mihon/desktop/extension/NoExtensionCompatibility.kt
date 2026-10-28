package mihon.desktop.extension

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.extension.kind.ExtensionCompatibility
import eu.kanade.tachiyomi.extension.kind.ExtensionFamilySupport
import eu.kanade.tachiyomi.extension.kind.UnsupportedSdk
import eu.kanade.tachiyomi.extension.model.Extension

/**
 * Desktop runs no extension family yet, so it installs no listing and the extensions screen has no compatibility to
 * show. Stands in until desktop hosts extensions.
 */
@Inject
class NoExtensionCompatibility : ExtensionCompatibility {
    override val families: List<ExtensionFamilySupport> = emptyList()

    override fun isInstallable(listing: Extension.Available): Boolean = false

    override fun unsupportedSdk(extension: Extension): UnsupportedSdk? = null
}
