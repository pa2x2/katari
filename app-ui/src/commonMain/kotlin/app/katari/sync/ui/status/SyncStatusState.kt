package app.katari.sync.ui.status

import app.katari.core.metro.AppCoroutineScope
import app.katari.sync.client.status.SyncStatus
import app.katari.sync.client.status.SyncStatusFlow
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * The one [SyncStatus] every place that shows sync reads (the Sync rows, the Library toolbar, the dot on More, the
 * notifications), so its many inputs are watched once however many of them show. Null until first read.
 */
@Inject
@SingleIn(AppScope::class)
class SyncStatusState(statusFlow: SyncStatusFlow, @AppCoroutineScope scope: CoroutineScope) {
    val status: StateFlow<SyncStatus?> =
        statusFlow.status().stateIn(scope, SharingStarted.WhileSubscribed(STOP_AFTER_MILLIS), null)

    private companion object {
        const val STOP_AFTER_MILLIS = 5_000L
    }
}
