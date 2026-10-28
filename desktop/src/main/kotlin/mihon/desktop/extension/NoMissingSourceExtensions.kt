package mihon.desktop.extension

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.ui.entry.missingsource.MissingSourceExtensions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Desktop can't install extensions yet, so an entry whose source is missing doesn't offer to install it. Stands in
 * until desktop hosts extensions.
 */
@Inject
class NoMissingSourceExtensions : MissingSourceExtensions {
    override val isOffered: Boolean = false

    override fun listing(sourceId: Long): Flow<Extension.Available?> = flowOf(null)

    override fun install(listing: Extension.Available): Flow<InstallStep> =
        error("Installing extensions is not available on desktop")
}
