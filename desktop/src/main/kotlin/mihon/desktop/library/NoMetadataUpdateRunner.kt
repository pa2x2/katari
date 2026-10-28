package mihon.desktop.library

import dev.zacsweers.metro.Inject
import tachiyomi.domain.library.service.MetadataUpdateRunner

/**
 * Stands in for refreshing the library's covers and details, which desktop cannot do until it can load sources. A
 * refresh starts and ends at once, as Android's ends for a library with nothing to refresh. A runner that refreshes on
 * desktop replaces this once desktop hosts extensions.
 */
@Inject
class NoMetadataUpdateRunner : MetadataUpdateRunner {
    override suspend fun startNow(): Boolean = true

    override suspend fun stop() = Unit
}
