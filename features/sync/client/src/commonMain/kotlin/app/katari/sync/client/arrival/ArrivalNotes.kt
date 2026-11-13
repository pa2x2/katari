package app.katari.sync.client.arrival

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.setting.value.SettingValue
import app.katari.sync.protocol.clock.HybridTimestamp
import kotlinx.serialization.protobuf.ProtoBuf
import tachiyomi.data.Database
import kotlin.time.Duration.Companion.days

/**
 * The settings one sync's records changed here from other devices, noted in its transaction with the value each
 * replaced ([SettingArrivals]). A setting changed again before Keep mine keeps the value it had before the first
 * change, which is the one that was this device's.
 */
internal class ArrivalNotes(private val database: Database, val batch: Long) {
    var noted = false
        private set

    suspend fun note(profileId: Long?, key: String, previous: Any?, at: HybridTimestamp) {
        val queries = database.sync_arrived_settingsQueries
        queries.deleteBefore(batch - KEPT_FOR.inWholeMilliseconds)
        val waiting = queries.waiting(profileId, key).awaitAsOneOrNull()
        if (waiting != null) {
            queries.changedAgain(
                fromDeviceId = at.device.value,
                changedAt = at.physicalMillis,
                batch = batch,
                id = waiting,
            )
        } else {
            queries.insert(
                profileId = profileId,
                settingKey = key,
                fromDeviceId = at.device.value,
                changedAt = at.physicalMillis,
                previous = ProtoBuf.encodeToByteArray(SettingValue.serializer(), SettingValue.of(previous)),
                batch = batch,
            )
        }
        noted = true
    }

    companion object {
        val KEPT_FOR = 14.days
    }
}
