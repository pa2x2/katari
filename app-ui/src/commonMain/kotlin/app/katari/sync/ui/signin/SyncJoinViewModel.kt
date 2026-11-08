package app.katari.sync.ui.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.sync.client.account.JoinResult
import app.katari.sync.client.account.LinkCodeLookup
import app.katari.sync.client.account.SyncAccount
import app.katari.sync.ui.address.AddressCheck
import app.katari.sync.ui.address.TypedAddressCheck
import app.katari.ui.di.AppUiScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.core.common.util.lang.launchIO

/**
 * Joining an account (D027): the server address is checked as it is typed, so the screen can warn about plain HTTP or
 * point to an empty server's web page before anyone signs in, and then the device signs in, redeems an invite, or
 * redeems a link code, which is looked up first so the person sees whose account it joins.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class SyncJoinViewModel(private val account: SyncAccount) : ViewModel() {
    private val typedAddress = TypedAddressCheck(viewModelScope, account::checkServer)

    val check: StateFlow<AddressCheck> = typedAddress.state

    val join: StateFlow<JoinState>
        field = MutableStateFlow<JoinState>(JoinState.Idle)

    val lookUp: StateFlow<LookUpState>
        field = MutableStateFlow<LookUpState>(LookUpState.Idle)

    /** Checks [address] once typing has paused. */
    fun addressChanged(address: String) = typedAddress.changed(address)

    /** Checks the address again, such as when the person comes back from the server's web page. */
    fun recheckAddress() = typedAddress.recheck()

    fun signIn(address: String, username: String, password: String) = joining(JoinPath.PASSWORD) {
        account.signIn(address, username, password)
    }

    fun redeemInvite(address: String, code: String, username: String, password: String) = joining(JoinPath.INVITE) {
        account.redeemInvite(address, code, username, password)
    }

    /** Finds which account [code] joins, without using it up, so joining can be asked about. */
    fun lookUpLinkCode(address: String, code: String) {
        if (lookUp.value == LookUpState.LookingUp) return
        lookUp.value = LookUpState.LookingUp
        viewModelScope.launchIO {
            lookUp.value = when (val found = account.lookUpLinkCode(address, code)) {
                is LinkCodeLookup.Found -> LookUpState.Found(found.accountName, found.fromDevice)
                is LinkCodeLookup.Failed -> LookUpState.Failed(found.result)
            }
        }
    }

    fun forgetLookUp() {
        lookUp.value = LookUpState.Idle
    }

    /** Forgets why the last look-up failed, such as when the person edits what they typed. */
    fun forgetFailedLookUp() {
        if (lookUp.value is LookUpState.Failed) lookUp.value = LookUpState.Idle
    }

    fun redeemLinkCode(address: String, code: String) = joining(JoinPath.LINK_CODE) {
        account.redeemLinkCode(address, code)
    }

    /** Forgets how the last attempt ended, such as when the person edits what they typed. */
    fun clearResult() {
        if (join.value is JoinState.Failed) join.value = JoinState.Idle
    }

    private fun joining(path: JoinPath, attempt: suspend () -> JoinResult) {
        if (join.value == JoinState.Joining) return
        join.value = JoinState.Joining
        viewModelScope.launchIO {
            val result = attempt()
            join.value = if (result is JoinResult.Joined) {
                JoinState.Joined(result.choicesWaiting)
            } else {
                JoinState.Failed(result, path)
            }
        }
    }

    sealed interface LookUpState {
        data object Idle : LookUpState

        data object LookingUp : LookUpState

        data class Found(val accountName: String, val fromDevice: String?) : LookUpState

        data class Failed(val result: JoinResult) : LookUpState
    }

    sealed interface JoinState {
        data object Idle : JoinState

        data object Joining : JoinState

        data class Joined(val choicesWaiting: Boolean) : JoinState

        data class Failed(val result: JoinResult, val path: JoinPath) : JoinState
    }
}
