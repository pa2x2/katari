package app.katari.sync.ui.status

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import app.katari.sync.client.status.SyncStatus
import app.katari.sync.ui.SyncUiGraph
import kotlinx.coroutines.delay
import mihon.app.ui.di.LocalAppUiGraph
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

@Composable
fun rememberSyncStatus(graph: SyncUiGraph = LocalAppUiGraph.current): SyncStatus? {
    val status by graph.syncStatusState.status.collectAsState()
    return status
}

/** The headline of the current status, read again as time passes, since "since yesterday" depends on the time. */
@Composable
fun rememberSyncHeadline(): SyncHeadline? {
    val status = rememberSyncStatus() ?: return null
    val now by produceState(Clock.System.now().toEpochMilliseconds()) {
        while (true) {
            delay(TICK)
            value = Clock.System.now().toEpochMilliseconds()
        }
    }
    return status.headline(now)
}

private val TICK = 30.seconds
