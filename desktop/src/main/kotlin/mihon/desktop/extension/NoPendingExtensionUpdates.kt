package mihon.desktop.extension

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.extension.PendingExtensionUpdates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Desktop loads no extensions yet, so none has an update to count. Stands in until desktop hosts extensions. */
@Inject
class NoPendingExtensionUpdates : PendingExtensionUpdates {
    override val count: Flow<Int> = flowOf(0)
    override val isAutoUpdateInProgress: Flow<Boolean> = flowOf(false)
}
