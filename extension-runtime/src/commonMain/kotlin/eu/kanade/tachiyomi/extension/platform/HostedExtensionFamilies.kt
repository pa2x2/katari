package eu.kanade.tachiyomi.extension.platform

import eu.kanade.tachiyomi.extension.kind.ExtensionFamily

/**
 * The extension families the platform can load. The stores' listings of the others are shown with why they can't be
 * installed, as listings of a library version the app doesn't run are, and the compatibility list leaves them out.
 */
interface HostedExtensionFamilies {
    val families: Set<ExtensionFamily>
}
