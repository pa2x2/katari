package mihon.feature.missingextensions.model

import eu.kanade.tachiyomi.extension.kind.ExtensionCompatibility
import eu.kanade.tachiyomi.extension.model.Extension

/** Why the extension a library needs can't be installed here. */
enum class CantInstall {
    /** No added store lists it: it's in a store not added here, or gone from every store. */
    NOT_LISTED,

    /** It finds torrents, which the app can't play. */
    TORRENT,

    /** This app, or this platform, can't run the listed version. */
    INCOMPATIBLE,
}

/** Why [listing] can't be installed here; null when it can. */
fun ExtensionCompatibility.cantInstall(listing: Extension.Available): CantInstall? = when {
    listing.isTorrent -> CantInstall.TORRENT
    !isInstallable(listing) -> CantInstall.INCOMPATIBLE
    else -> null
}
