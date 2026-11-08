package app.katari.sync.ui.page

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.sync.client.account.AccountCall
import app.katari.sync.client.account.AccountDevice
import app.katari.sync.client.account.SyncAccount
import app.katari.sync.client.connection.DeviceNaming
import app.katari.sync.client.connection.DeviceTokenStore
import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.client.status.SyncStatus
import app.katari.sync.ui.status.SyncStatusState
import app.katari.ui.di.AppUiScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import tachiyomi.core.common.util.lang.launchIO

/** The Sync page (D050): the status, the account's devices, the options, and leaving. */
@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class SyncViewModel(
    private val account: SyncAccount,
    private val tokens: DeviceTokenStore,
    statusState: SyncStatusState,
    val preferences: SyncPreferences,
    val naming: DeviceNaming,
) : ViewModel() {
    val status: StateFlow<SyncStatus?> = statusState.status

    val devices: StateFlow<DevicesState>
        field = MutableStateFlow<DevicesState>(DevicesState.Loading)

    /** Whether this device has somewhere to keep its sign-in; false on a desktop without a keyring (D061). */
    val canKeepSignIn: StateFlow<Boolean?>
        field = MutableStateFlow<Boolean?>(null)

    private val removeFailures = MutableStateFlow(0)

    /** Counts removals that failed, so each one shows its message once. */
    val removeFailed: StateFlow<Int> = removeFailures.asStateFlow()

    init {
        viewModelScope.launchIO { canKeepSignIn.value = tokens.isAvailable }
        // Listed again when the device links to another account, stops or signs in again, even while the first sync
        // after that runs, which can take minutes; after each sync, which is when devices join and when the server
        // notes when each last synced; and once the server can't be reached, saying so. A sync starting changes none.
        viewModelScope.launch {
            status.filterNotNull()
                .map { DevicesKey(it.link as? SyncStatus.Link.Linked, it.activity) }
                .distinctUntilChanged { listed, now ->
                    listed.link == now.link &&
                        (now.activity is SyncStatus.Activity.Syncing || listed.ended == now.ended)
                }
                .collect { refreshDevices() }
        }
        // And as soon as the server says another device joined or left (D063)
        viewModelScope.launch {
            account.deviceListChanges.drop(1).collect { refreshDevices() }
        }
    }

    fun refreshDevices() {
        viewModelScope.launchIO {
            devices.value = when (val call = account.devices()) {
                is AccountCall.Done -> DevicesState.Listed(call.value.sortedByDescending { it.isThisDevice })
                AccountCall.NotLinked -> DevicesState.Loading
                AccountCall.Unreachable, is AccountCall.Refused -> DevicesState.Unavailable
            }
        }
    }

    fun syncNow() {
        viewModelScope.launchIO { account.syncNow() }
    }

    fun removeDevice(device: AccountDevice) {
        viewModelScope.launchIO {
            val removed = account.removeDevice(device.id)
            if (removed !is AccountCall.Done) removeFailures.value++
            refreshDevices()
        }
    }

    /** Names this device [name], in its row at once and in the account with the sync this starts. */
    fun renameThisDevice(name: String) {
        viewModelScope.launchIO {
            naming.rename(name)
            (devices.value as? DevicesState.Listed)?.let { listed ->
                val named = naming.name()
                devices.value = DevicesState.Listed(
                    listed.devices.map { device ->
                        if (!device.isThisDevice) return@map device
                        val description = device.device.description.copy(name = named)
                        device.copy(device = device.device.copy(description = description))
                    },
                )
            }
            account.syncNow()
        }
    }

    fun disconnect() {
        viewModelScope.launchIO {
            account.disconnect()
            devices.value = DevicesState.Loading
        }
    }

    fun dismissEnded() {
        viewModelScope.launchIO { account.dismissNotice() }
    }

    private class DevicesKey(val link: SyncStatus.Link.Linked?, val activity: SyncStatus.Activity) {
        // The last sync that got through while the status shows it, null while it shows anything else
        val ended = activity as? SyncStatus.Activity.UpToDate
    }

    sealed interface DevicesState {
        data object Loading : DevicesState

        data class Listed(val devices: List<AccountDevice>) : DevicesState

        data object Unavailable : DevicesState
    }
}
