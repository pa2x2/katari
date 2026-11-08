package app.katari.sync.client.firstsync

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.katari.sync.client.kind.profile.ProfileKind
import app.katari.sync.client.outbox.OutboxDrain
import app.katari.sync.client.setting.read.SettingReads
import dev.zacsweers.metro.Inject
import tachiyomi.data.DatabaseHandler

/**
 * Records the person-level preferences a device held before it joined (step 5 of [FirstSync]), after the account's
 * records are applied: like [ExistingRows], with no edit time, so a key the account had, which now holds the account's
 * value, changes nothing, and a key only this device set reaches the account.
 */
@Inject
class ExistingSettings(
    private val handler: DatabaseHandler,
    private val settings: SettingReads,
    private val drain: OutboxDrain,
) {
    suspend fun record() {
        handler.await(inTransaction = true) {
            sync_local_rowsQueries.profiles().awaitAsList().forEach { profile ->
                val isDefault = profile.uuid == ProfileKind.DEFAULT_UUID
                settings.personLevelSettings(profile._id, isDefault).forEach { (kind, ref) ->
                    sync_outboxQueries.recordUnstamped(kind = kind, localRef = ref.json)
                }
            }
        }
        do {
            val drained = drain.drain()
        } while (drained > 0)
    }
}
