package app.katari.sync.client.arrival

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.setting.value.SettingValue
import app.katari.sync.protocol.clock.HybridTimestamp
import kotlinx.serialization.protobuf.ProtoBuf
import tachiyomi.data.Database
import kotlin.time.Duration.Companion.days

/**
 * The settings one sync's records changed here from other devices, noted in its transaction with the value each
 * replaced and the value it arrived with ([SettingArrivals]). A setting changed again from elsewhere before Keep mine
 * keeps the value it had before the first change, which is the one that was this device's, until this device changes it
 * itself ([app.katari.sync.client.outbox.OutboxDrain]). A note is kept for [KEPT_FOR] after the last sync that changed
 * its setting, however long before that the other device made the change.
 */
internal class ArrivalNotes(private val database: Database, val batch: Long) {
    var noted = false
        private set

    suspend fun note(profileId: Long?, key: String, previous: Any?, arrived: Any?, at: HybridTimestamp) {
        val queries = database.sync_arrived_settingsQueries
        queries.deleteBefore(batch - KEPT_FOR.inWholeMilliseconds)
        val waiting = queries.waiting(profileId, key).awaitAsOneOrNull()
        if (waiting != null) {
            queries.changedAgain(
                fromDeviceId = at.device.value,
                arrived = encode(arrived),
                batch = batch,
                id = waiting,
            )
        } else {
            queries.insert(
                profileId = profileId,
                settingKey = key,
                fromDeviceId = at.device.value,
                previous = encode(previous),
                arrived = encode(arrived),
                batch = batch,
            )
        }
        noted = true
    }

    private fun encode(stored: Any?) = ProtoBuf.encodeToByteArray(SettingValue.serializer(), SettingValue.of(stored))

    companion object {
        val KEPT_FOR = 14.days
    }
}
