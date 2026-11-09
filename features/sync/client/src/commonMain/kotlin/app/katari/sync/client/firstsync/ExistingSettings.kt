package app.katari.sync.client.firstsync

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.katari.sync.client.kind.profile.ProfileKind
import app.katari.sync.client.outbox.OutboxDrain
import app.katari.sync.client.outbox.recordUnstamped
import app.katari.sync.client.setting.read.SettingReads
import app.katari.sync.client.setting.read.settingRecords
import dev.zacsweers.metro.Inject
import tachiyomi.data.DatabaseHandler

/**
 * Records the settings that can sync a device held before it joined (step 5 of [FirstSync]), after the account's
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
        val profiles = handler.await { sync_local_rowsQueries.profiles().awaitAsList() }
        handler.recordUnstamped(
            profiles.flatMap {
                settings.settingRecords(it._id, isDefaultProfile = it.uuid == ProfileKind.DEFAULT_UUID)
            },
        )
        do {
            val drained = drain.drain()
        } while (drained > 0)
    }
}
