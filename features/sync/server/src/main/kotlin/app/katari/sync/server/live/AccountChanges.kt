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

/**
 * What the live channel tells connected devices: an account's sequence moving on, a device joining or leaving the
 * account, a message coming for a device, and devices being revoked or their account disabled, either of which closes
 * their connection.
 */
class AccountChanges {
    private val sequences = ConcurrentHashMap<Long, MutableStateFlow<Long>>()
    private val deviceCounts = ConcurrentHashMap<Long, MutableStateFlow<Long>>()
    private val newestMessages = ConcurrentHashMap<DeviceId, MutableStateFlow<Long>>()
    private val revoked = MutableSharedFlow<DeviceId>(extraBufferCapacity = 64)
    private val disabled = MutableSharedFlow<Long>(extraBufferCapacity = 64)

    /** Holds 0 until the first [announce] for the account, not its stored sequence. */
    fun sequence(accountId: Long): StateFlow<Long> = flowOf(accountId).asStateFlow()

    /** How many times a device joined or left the account since the server started. */
    fun devices(accountId: Long): StateFlow<Long> = deviceCountOf(accountId).asStateFlow()

    /** The newest message number sent to the device since the server started, 0 before the first. */
    fun newestMessage(device: DeviceId): StateFlow<Long> = newestMessageOf(device).asStateFlow()

    val revocations: SharedFlow<DeviceId> = revoked.asSharedFlow()

    /** Account ids. */
    val disabledAccounts: SharedFlow<Long> = disabled.asSharedFlow()

    /** Called once a write that gave out [sequence] has committed. */
    fun announce(accountId: Long, sequence: Long) {
        flowOf(accountId).update { maxOf(it, sequence) }
    }

    /** Called once a device joining or leaving the account has committed. */
    fun announceDevicesChanged(accountId: Long) {
        deviceCountOf(accountId).update { it + 1 }
    }

    /** Called once the message [number] for [device] has committed. */
    fun announceMessage(device: DeviceId, number: Long) {
        newestMessageOf(device).update { maxOf(it, number) }
    }

    fun announceRevoked(device: DeviceId) {
        revoked.tryEmit(device)
    }

    fun announceDisabled(accountId: Long) {
        disabled.tryEmit(accountId)
    }

    private fun flowOf(accountId: Long) = sequences.computeIfAbsent(accountId) { MutableStateFlow(0) }

    private fun deviceCountOf(accountId: Long) = deviceCounts.computeIfAbsent(accountId) { MutableStateFlow(0) }

    private fun newestMessageOf(device: DeviceId) = newestMessages.computeIfAbsent(device) { MutableStateFlow(0) }
}
