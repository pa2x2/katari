package app.katari.sync.server.live

import app.katari.sync.protocol.device.DeviceId
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

/** What the live channel tells connected devices: an account's sequence moving on, and devices being revoked. */
class AccountChanges {
    private val sequences = ConcurrentHashMap<Long, MutableStateFlow<Long>>()
    private val revoked = MutableSharedFlow<DeviceId>(extraBufferCapacity = 64)

    /** Holds 0 until the first [announce] for the account, not its stored sequence. */
    fun sequence(accountId: Long): StateFlow<Long> = flowOf(accountId).asStateFlow()

    val revocations: SharedFlow<DeviceId> = revoked.asSharedFlow()

    /** Called once a write that gave out [sequence] has committed. */
    fun announce(accountId: Long, sequence: Long) {
        flowOf(accountId).update { maxOf(it, sequence) }
    }

    fun announceRevoked(device: DeviceId) {
        revoked.tryEmit(device)
    }

    private fun flowOf(accountId: Long) = sequences.computeIfAbsent(accountId) { MutableStateFlow(0) }
}
