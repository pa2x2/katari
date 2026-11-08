package app.katari.sync.client.file.transfer

import app.katari.sync.client.preference.SyncPreferences
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import mihon.core.platform.network.NetworkConnectivity

/**
 * When custom covers and fonts may move (D022): on Wi-Fi, or on any network once the person allows mobile data.
 * Records don't wait for it; only the records naming a file that waits to be uploaded do.
 */
@Inject
class FileTransferRule(
    private val network: NetworkConnectivity,
    private val preferences: SyncPreferences,
) {
    fun allowsNow(): Boolean = preferences.useMobileData.get() || network.current().countsAsWifi

    fun allows(): Flow<Boolean> = combine(network.state(), preferences.useMobileData.changes()) { state, mobile ->
        mobile || state.countsAsWifi
    }
}
