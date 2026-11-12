package app.katari.sync.client.account

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

/**
 * Keeps the account's devices as this device last listed them ([DeviceDirectory]) up with the account. The live channel
 * announces each device that joins, leaves or describes itself anew, as one does after an update that lets it take a
 * new kind of message; each announcement lists them again. The first one comes as the channel opens, which catches up
 * on what changed while it was closed.
 */
@Inject
@SingleIn(AppScope::class)
class DeviceListFollowing(private val changes: DeviceListChanges, private val account: SyncAccount) {
    /** Follows the announcements until cancelled; a listing that fails waits for the next one. */
    suspend fun follow() {
        changes.heard.drop(1).collectLatest { account.devices() }
    }
}
