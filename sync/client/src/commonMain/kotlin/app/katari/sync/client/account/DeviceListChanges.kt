package app.katari.sync.client.account

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The live channel's count of devices joining or leaving the account (D063), so a screen listing the account's devices
 * lists them again when another device links or leaves, not only after the next sync.
 */
@Inject
@SingleIn(AppScope::class)
class DeviceListChanges {
    private val count = MutableStateFlow(0L)

    /** The latest count the server announced; it only means something as it changes. */
    val heard: StateFlow<Long> = count.asStateFlow()

    internal fun hear(devices: Long) {
        count.value = devices
    }
}
