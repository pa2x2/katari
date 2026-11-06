package app.katari.sync.ui.link

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.sync.client.account.AccountCall
import app.katari.sync.client.account.LinkCodeOffer
import app.katari.sync.client.account.SyncAccount
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mihon.app.di.AppUiScope
import tachiyomi.core.common.util.lang.launchIO
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

/** The code Link a device shows (D024, D027): valid once, for two minutes. */
@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class LinkDeviceViewModel(private val account: SyncAccount) : ViewModel() {
    val code: StateFlow<LinkCodeState>
        field = MutableStateFlow<LinkCodeState>(LinkCodeState.Loading)

    fun newCode() {
        code.value = LinkCodeState.Loading
        viewModelScope.launchIO {
            val asked = Clock.System.now().toEpochMilliseconds()
            code.value = when (val call = account.createLinkCode()) {
                is AccountCall.Done -> LinkCodeState.Shown(
                    offer = call.value,
                    // The server's clock may run ahead of this one; the code is never shown for longer than it lives.
                    expiresAtMillis = minOf(call.value.expiresAtMillis, asked + LIFETIME.inWholeMilliseconds),
                )
                else -> LinkCodeState.Unavailable
            }
        }
    }

    sealed interface LinkCodeState {
        data object Loading : LinkCodeState

        /** [expiresAtMillis] on this device's clock. */
        data class Shown(val offer: LinkCodeOffer, val expiresAtMillis: Long) : LinkCodeState

        data object Unavailable : LinkCodeState
    }

    private companion object {
        val LIFETIME = 2.minutes
    }
}
