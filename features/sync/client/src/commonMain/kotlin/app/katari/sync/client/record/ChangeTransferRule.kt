package app.katari.sync.client.record

import app.katari.core.platform.network.NetworkConnectivity
import app.katari.sync.client.preference.SyncPreferences
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * When changes may be pulled and pushed: on Wi-Fi, or on any network once the person allows changes on mobile data.
 * Messages between devices don't wait for it, being few and small, and neither does the live channel that brings them.
 */
@Inject
class ChangeTransferRule(
    private val network: NetworkConnectivity,
    private val preferences: SyncPreferences,
) {
    fun allowsNow(): Boolean = preferences.changesOnMobileData.get() || network.current().countsAsWifi

    fun allows(): Flow<Boolean> = combine(network.state(), preferences.changesOnMobileData.changes()) { state, mobile ->
        mobile || state.countsAsWifi
    }
        .distinctUntilChanged()
}
