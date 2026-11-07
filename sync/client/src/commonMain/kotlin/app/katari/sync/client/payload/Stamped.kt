package app.katari.sync.client.payload

import app.katari.sync.protocol.clock.HybridTimestamp
import app.katari.sync.protocol.device.DeviceId
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/** A synced field's value and when it was last edited; merging keeps the value with the later [at]. */
@Serializable
data class Stamped<T>(
    @ProtoNumber(1) val value: T,
    @ProtoNumber(2) val at: HybridTimestamp,
) {
    companion object {
        /**
         * For a field this device holds a value for but never saw edited while sync was on, such as one set before
         * sync was set up. It sorts before every real timestamp, so any edit made anywhere wins over it.
         */
        val UNKNOWN_EDIT: HybridTimestamp = HybridTimestamp(physicalMillis = 0, counter = 0, device = DeviceId(0))
    }
}
