package app.katari.sync.ui.link

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.sync.client.account.AccountCall
import app.katari.sync.client.account.LinkCodeOffer
import app.katari.sync.client.account.SyncAccount
import app.katari.sync.protocol.device.DeviceId
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mihon.app.di.AppUiScope
import tachiyomi.core.common.util.lang.launchIO
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * The code Link a device shows (D024, D027): valid once, for two minutes. The server forgets a code once it is used and
 * tells this device nothing, so while the code is shown the account's devices are listed again every few seconds; one
 * that wasn't there when the code was made is the device that used it.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class LinkDeviceViewModel(private val account: SyncAccount) : ViewModel() {
    val code: StateFlow<LinkCodeState>
        field = MutableStateFlow<LinkCodeState>(LinkCodeState.Loading)

    private var offering: Job? = null

    fun newCode() {
        offering?.cancel()
        code.value = LinkCodeState.Loading
        offering = viewModelScope.launchIO {
            val before = deviceIds()
            val asked = Clock.System.now().toEpochMilliseconds()
            val shown = when (val call = account.createLinkCode()) {
                is AccountCall.Done -> LinkCodeState.Shown(
                    offer = call.value,
                    // The server's clock may run ahead of this one; the code is never shown for longer than it lives.
                    expiresAtMillis = minOf(call.value.expiresAtMillis, asked + LIFETIME.inWholeMilliseconds),
                )
                else -> LinkCodeState.Unavailable
            }
            code.value = shown
            if (shown is LinkCodeState.Shown && before != null) code.value = awaitUse(shown, before)
        }
    }

    private suspend fun deviceIds(): Set<DeviceId>? =
        (account.devices() as? AccountCall.Done)?.value?.mapTo(mutableSetOf()) { it.id }

    /** Waits for a new device until shortly after the code expired, when one that used it last has shown up. */
    private suspend fun awaitUse(shown: LinkCodeState.Shown, before: Set<DeviceId>): LinkCodeState {
        while (Clock.System.now().toEpochMilliseconds() < shown.expiresAtMillis + WATCH.inWholeMilliseconds) {
            delay(WATCH)
            val joined = (account.devices() as? AccountCall.Done)?.value?.firstOrNull { it.id !in before }
            if (joined != null) return LinkCodeState.Used(joined.device.description.name)
        }
        return shown
    }

    sealed interface LinkCodeState {
        data object Loading : LinkCodeState

        /** [expiresAtMillis] on this device's clock. */
        data class Shown(val offer: LinkCodeOffer, val expiresAtMillis: Long) : LinkCodeState

        data object Unavailable : LinkCodeState

        /** A device named [deviceName] joined the account, with this code as far as this device can tell. */
        data class Used(val deviceName: String) : LinkCodeState
    }

    private companion object {
        val LIFETIME = 2.minutes
        val WATCH = 3.seconds
    }
}
