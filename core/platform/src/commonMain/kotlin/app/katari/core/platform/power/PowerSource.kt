package app.katari.core.platform.power

import app.katari.core.common.platform.PlatformFacility
import kotlinx.coroutines.flow.Flow

/**
 * Where the device draws its power from, as background work limited to charging waits for it, and how charged its
 * battery is, as a reader's status line shows it.
 */
@PlatformFacility
interface PowerSource {
    /**
     * Whether the device is on a charger, which also counts once the battery is full, now and then at each change, for
     * as long as it is collected.
     */
    fun isPluggedIn(): Flow<Boolean>

    /**
     * The battery's charge in percent, now and then at each change, for as long as it is collected; null on a device
     * without a battery, or while the system hasn't reported it yet.
     */
    fun batteryPercent(): Flow<Int?>
}
