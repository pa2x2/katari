package eu.kanade.tachiyomi.extension.store

import eu.kanade.tachiyomi.extension.kind.ExtensionKinds
import eu.kanade.tachiyomi.extension.model.Extension

/**
 * Stores list extensions this build can't run too: torrent extensions and ones built against a library version it
 * doesn't load. They're shown, so the user sees why, but never installed or offered as updates.
 */
internal val Extension.Available.isInstallable: Boolean
    get() = !isTorrent && ExtensionKinds.runsListedLib(sourceIdScheme, libVersionName)
