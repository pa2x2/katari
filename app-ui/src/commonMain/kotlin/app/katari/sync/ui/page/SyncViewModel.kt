package app.katari.sync.ui.page

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.core.metro.AppCoroutineScope
import app.katari.sync.client.account.AccountCall
import app.katari.sync.client.account.AccountDevice
import app.katari.sync.client.account.SyncAccount
import app.katari.sync.client.connection.DeviceNaming
import app.katari.sync.client.connection.DeviceTokenStore
import app.katari.sync.client.part.ChoiceSet
import app.katari.sync.client.part.ProfileSync
import app.katari.sync.client.part.ProfileSyncState
import app.katari.sync.client.part.ProfileSyncStates
import app.katari.sync.client.part.SyncChoiceChanges
import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.client.status.SyncStatus
import app.katari.sync.ui.status.SyncStatusState
import app.katari.sync.ui.testmessage.TestMessages
import app.katari.ui.buildinfo.AppBuild
import app.katari.ui.buildinfo.AppBuildType
import app.katari.ui.di.AppUiScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val choiceChanges: SyncChoiceChanges,
    profileStates: ProfileSyncStates,
    @AppCoroutineScope private val appScope: CoroutineScope,
    private val testMessages: TestMessages,
    build: AppBuild,
) : ViewModel() {
    /** Debug builds send test messages to the account's other devices ([TestMessages]). */
    val sendsTestMessages: Boolean = build.type == AppBuildType.Debug

    val status: StateFlow<SyncStatus?> = statusState.status

    val choices: StateFlow<ChoiceSet> =
        preferences.choicesChanges().stateIn(viewModelScope, SharingStarted.Eagerly, preferences.choices())

    /** The profiles this device doesn't sync: kept to itself, or made on another device and waiting for an answer. */
    val profilesNotSynced: StateFlow<List<ProfileSync>> = profileStates.profiles()
        .map { profiles -> profiles.filter { it.state != ProfileSyncState.SYNCED } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Whether the last listing since the page opened failed, which matters only while none was ever kept
    private val listingFailed = MutableStateFlow(false)

    /**
     * The account's devices as last listed, at once, then as the server lists them again; null until the kept list is
     * read, so the page doesn't show an empty one first.
     */
    val devices: StateFlow<DevicesState?> = combine(account.listedDevices(), listingFailed) { listed, failed ->
        when {
            listed.isNotEmpty() -> DevicesState.Listed(listed.sortedByDescending { it.isThisDevice })
            failed -> DevicesState.Unavailable
            else -> DevicesState.Loading
        }
    }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

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

    /** In the app's scope, so leaving the page doesn't stop bringing the profile here after the answer was saved. */
    fun answerArrivingProfile(profile: ProfileSync, comesHere: Boolean) {
        appScope.launchIO { choiceChanges.syncProfile(profile.uuid, on = comesHere) }
    }

    fun refreshDevices() {
        viewModelScope.launchIO {
            val call = account.devices()
            listingFailed.value = call is AccountCall.Unreachable || call is AccountCall.Refused
        }
    }

    fun syncNow() {
        viewModelScope.launchIO { account.syncNow() }
    }

    fun sendTestMessage(device: AccountDevice) {
        viewModelScope.launchIO { testMessages.send(device) }
    }

    fun removeDevice(device: AccountDevice) {
        viewModelScope.launchIO {
            val removed = account.removeDevice(device.id)
            if (removed !is AccountCall.Done) removeFailures.value++
            refreshDevices()
        }
    }

    fun renameThisDevice(name: String) {
        viewModelScope.launchIO { account.renameThisDevice(name) }
    }

    fun disconnect() {
        viewModelScope.launchIO {
            account.disconnect()
            listingFailed.value = false
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
