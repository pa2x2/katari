package mihon.desktop.extension

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.extension.AvailableExtensionsRefresh

/**
 * Desktop loads no extensions yet, so there is no list of what the stores offer to read again when they change. Stands
 * in until desktop hosts extensions.
 */
@Inject
class NoAvailableExtensionsRefresh : AvailableExtensionsRefresh {
    override suspend fun refresh() = Unit
}
