package app.katari.sync.ui.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.sync.client.account.JoinResult
import app.katari.sync.client.account.LinkCodeLookup
import app.katari.sync.client.account.ServerCheck
import app.katari.sync.client.account.SyncAccount
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mihon.app.di.AppUiScope
import tachiyomi.core.common.util.lang.launchIO
import kotlin.time.Duration.Companion.milliseconds

/**
 * Joining an account (D027): the server address is checked as it is typed, so the screen can warn about plain HTTP or
 * point to an empty server's web page before anyone signs in, and then the device signs in, redeems an invite, or
 * redeems a link code, which is looked up first so the person sees whose account it joins.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class SyncJoinViewModel(private val account: SyncAccount) : ViewModel() {
    val check: StateFlow<AddressCheck>
        field = MutableStateFlow<AddressCheck>(AddressCheck.Empty)

    val join: StateFlow<JoinState>
        field = MutableStateFlow<JoinState>(JoinState.Idle)

    val lookUp: StateFlow<LookUpState>
        field = MutableStateFlow<LookUpState>(LookUpState.Idle)

    private var checking: Job? = null
    private var checkedAddress: String? = null

    /** Checks [address] once typing has paused. */
    fun addressChanged(address: String) {
        val trimmed = address.trim()
        if (trimmed == checkedAddress) return
        checkedAddress = trimmed
        checking?.cancel()
        if (trimmed.isEmpty()) {
            check.value = AddressCheck.Empty
            return
        }
        check.value = AddressCheck.Checking
        checking = viewModelScope.launchIO {
            delay(TYPING_PAUSE)
            check.value = AddressCheck.Checked(account.checkServer(trimmed))
        }
    }

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
            join.value = if (result == JoinResult.Joined) JoinState.Joined else JoinState.Failed(result, path)
        }
    }

    sealed interface AddressCheck {
        data object Empty : AddressCheck

        data object Checking : AddressCheck

        data class Checked(val check: ServerCheck) : AddressCheck
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

        data object Joined : JoinState

        data class Failed(val result: JoinResult, val path: JoinPath) : JoinState
    }

    private companion object {
        val TYPING_PAUSE = 600.milliseconds
    }
}
