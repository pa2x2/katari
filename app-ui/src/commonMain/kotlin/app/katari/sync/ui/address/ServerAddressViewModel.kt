package app.katari.sync.ui.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.sync.client.account.ServerCheck
import app.katari.sync.client.account.ServerMove
import app.katari.sync.client.account.SyncAccount
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import mihon.app.di.AppUiScope
import tachiyomi.core.common.util.lang.launchIO

/**
 * Following this device's server to a new address: the address is checked as it is typed, as at sign-in, and
 * the server found there is told apart from the one the device syncs with before it moves.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class ServerAddressViewModel(private val account: SyncAccount) : ViewModel() {
    private val typedAddress = TypedAddressCheck(viewModelScope, account::checkServer)

    val check: StateFlow<AddressCheck> = typedAddress.state

    /** Whether the server a check found ready is the one this device syncs with; null until one is found. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val isThisServer: StateFlow<Boolean?> = check
        .mapLatest { shown ->
            ((shown as? AddressCheck.Checked)?.check as? ServerCheck.Ready)?.let { account.isThisServer(it) }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val move: StateFlow<MoveState>
        field = MutableStateFlow<MoveState>(MoveState.Idle)

    /** Checks [address] once typing has paused; an empty one is not checked. */
    fun addressChanged(address: String) {
        if (move.value is MoveState.Failed) move.value = MoveState.Idle
        typedAddress.changed(address)
    }

    fun moveTo(address: String) {
        if (move.value == MoveState.Moving) return
        move.value = MoveState.Moving
        viewModelScope.launchIO {
            move.value = when (val moved = account.moveServer(address)) {
                ServerMove.Moved -> MoveState.Moved
                else -> MoveState.Failed(moved)
            }
        }
    }

    sealed interface MoveState {
        data object Idle : MoveState

        data object Moving : MoveState

        data object Moved : MoveState

        data class Failed(val result: ServerMove) : MoveState
    }
}
