package mihon.entry.interactions.download

import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.selects.select
import mihon.core.platform.network.NetworkConnectivity
import tachiyomi.domain.download.service.DownloadPreferences

/**
 * One run of the download queue in the background, as each platform's download work starts it: it downloads until the
 * queue is empty. Downloads go only while the network allows them, online and, while they are limited to Wi-Fi,
 * unmetered; when it stops allowing them, the transfers stop where they are and go on once it allows them again.
 */
class EntryDownloadRun internal constructor(
    private val downloads: EntryDownloadRuntimeCoordinator,
    private val downloadPreferences: DownloadPreferences,
    private val network: NetworkConnectivity,
) {
    /**
     * @param onPending called each time downloads are pending, before they wait for the network; Android's worker
     * goes to the foreground there.
     */
    suspend fun run(onPending: suspend () -> Unit = {}) {
        while (true) {
            if (!downloads.hasPendingDownloads()) return
            onPending()
            allowedNetwork().first { it }
            if (runUntilNetworkBlocked()) return
        }
    }

    /** Downloads until the queue is idle, which returns true, or until the network stops allowing it, false. */
    private suspend fun runUntilNetworkBlocked(): Boolean = coroutineScope {
        val processing = async { downloads.runDownloadsUntilIdle() }
        val networkBlocked = async { allowedNetwork().first { !it } }
        select {
            processing.onAwait {
                networkBlocked.cancelAndJoin()
                true
            }
            networkBlocked.onAwait {
                processing.cancelAndJoin()
                false
            }
        }
    }

    private fun allowedNetwork(): Flow<Boolean> = combine(
        network.state(),
        flow {
            emit(downloadPreferences.downloadOnlyOverWifi.get())
            emitAll(downloadPreferences.downloadOnlyOverWifi.changes())
        },
    ) { network, requireWifi ->
        isEntryDownloadNetworkAllowed(network.isOnline, network.isUnmetered, requireWifi)
    }
}
