package mihon.core.platform.power

import kotlinx.coroutines.flow.Flow

/** Where the device draws its power from, as background work limited to charging waits for it. */
interface PowerSource {
    /**
     * Whether the device is on a charger, which also counts once the battery is full, now and then at each change, for
     * as long as it is collected.
     */
    fun isPluggedIn(): Flow<Boolean>
}
