package app.katari.sync.ui.address

import app.katari.sync.client.account.ServerCheck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tachiyomi.core.common.util.lang.launchIO
import kotlin.time.Duration.Companion.milliseconds

/**
 * A server address checked as it is typed (D027), so a screen can warn about plain HTTP, point to an empty server's
 * web page or refuse an address before anyone signs in: once typing has paused, and again on [recheck].
 */
internal class TypedAddressCheck(
    private val scope: CoroutineScope,
    private val check: suspend (address: String) -> ServerCheck,
) {
    private val shown = MutableStateFlow<AddressCheck>(AddressCheck.Empty)
    val state: StateFlow<AddressCheck> = shown.asStateFlow()

    private var checking: Job? = null
    private var checkedAddress: String? = null

    fun changed(address: String) {
        val trimmed = address.trim()
        if (trimmed == checkedAddress) return
        checkedAddress = trimmed
        checking?.cancel()
        if (trimmed.isEmpty()) {
            shown.value = AddressCheck.Empty
            return
        }
        shown.value = AddressCheck.Checking
        checking = scope.launchIO {
            delay(TYPING_PAUSE)
            shown.value = AddressCheck.Checked(check(trimmed))
        }
    }

    /**
     * Checks the address again, keeping what the last check found on screen meanwhile; the server may have changed
     * since, such as an empty one whose admin account the person just made on its web page (D025).
     */
    fun recheck() {
        val address = checkedAddress ?: return
        if (shown.value !is AddressCheck.Checked) return
        checking?.cancel()
        checking = scope.launchIO { shown.value = AddressCheck.Checked(check(address)) }
    }

    private companion object {
        val TYPING_PAUSE = 600.milliseconds
    }
}

sealed interface AddressCheck {
    data object Empty : AddressCheck

    data object Checking : AddressCheck

    data class Checked(val check: ServerCheck) : AddressCheck
}
