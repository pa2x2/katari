package app.katari.sync.client.arrival

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.katari.sync.client.account.DeviceDirectory
import app.katari.sync.client.part.ChoiceSet
import app.katari.sync.client.part.SyncChoiceChanges
import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.client.setting.apply.SettingWrites
import app.katari.sync.client.setting.scope.PreferenceScopes
import app.katari.sync.client.setting.value.SettingValue
import app.katari.sync.data.Sync_arrived_settings
import app.katari.sync.protocol.device.DeviceId
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.protobuf.ProtoBuf
import tachiyomi.data.DatabaseHandler
import kotlin.time.Clock

/**
 * The settings other devices changed here in the last 14 days, and Keep mine: it keeps the setting on this device,
 * then puts back the value it had. Kept here first, the value written back is held back like any edit of a setting
 * this device doesn't sync, and switching the setting on again later asks whose value it keeps.
 *
 * A setting of a menu ([ArrivedSetting.menu]) is kept with the rest of its menu that the same sync changed, as its
 * settings sync or stay as one.
 */
@Inject
class SettingArrivals(
    private val handler: DatabaseHandler,
    private val directory: DeviceDirectory,
    private val choices: SyncChoiceChanges,
    private val preferences: SyncPreferences,
    private val settings: SettingWrites,
    private val batches: ArrivedBatches,
) {
    /** Every setting changed here in the last 14 days, newest first, as this device's choices stand. */
    fun changes(): Flow<List<ArrivedSetting>> = combine(
        handler.subscribeToList { sync_arrived_settingsQueries.all() },
        preferences.choicesChanges(),
    ) { rows, choices ->
        val since = now() - ArrivalNotes.KEPT_FOR.inWholeMilliseconds
        val names = directory.listed()
        rows.filter { it.changed_at >= since }.map { it.arrived(names, choices) }
    }

    /** The settings each sync changes here from now on, as that sync is committed. */
    fun arrivals(): Flow<List<ArrivedSetting>> = batches.batches.map { batch ->
        val names = directory.listed()
        val choices = preferences.choices()
        handler.await { sync_arrived_settingsQueries.inBatch(batch).awaitAsList() }.map { it.arrived(names, choices) }
    }

    /** Keeps [setting] on this device with the value it had, along with the rest of its menu that its sync changed. */
    suspend fun keepMine(setting: ArrivedSetting) {
        val rows = handler.await { sync_arrived_settingsQueries.inBatch(setting.batch).awaitAsList() }
        keep(rows.filter { it._id == setting.id || (setting.menu != null && it.arrivedMenu == setting.menu) })
    }

    /** Keeps every setting [batch]'s sync changed here, as a notice of them offers. */
    suspend fun keepMine(batch: Long) {
        keep(handler.await { sync_arrived_settingsQueries.inBatch(batch).awaitAsList() })
    }

    private suspend fun keep(rows: List<Sync_arrived_settings>) {
        if (rows.isEmpty()) return
        val menus = rows.mapNotNull { it.arrivedMenu }.toSet()
        menus.forEach { choices.syncMenu(it, on = false) }
        rows.filter { it.arrivedMenu == null }.map { it.setting_key }.toSet().forEach {
            choices.syncSetting(it, on = false)
        }
        rows.forEach { row ->
            val previous = ProtoBuf.decodeFromByteArray(SettingValue.serializer(), row.previous).stored
            val profileId = row.profile_id
            if (profileId == null) {
                settings.writeSharedValue(row.setting_key, previous)
            } else {
                settings.writeProfileValue(profileId, row.setting_key, previous)
            }
        }
        handler.await { sync_arrived_settingsQueries.markKept(rows.map { it._id }) }
    }

    private val Sync_arrived_settings.arrivedMenu
        get() = PreferenceScopes.markOf(setting_key)?.menu

    private fun Sync_arrived_settings.arrived(names: Map<DeviceId, String>, choices: ChoiceSet) = ArrivedSetting(
        id = _id,
        profileId = profile_id,
        key = setting_key,
        from = names[DeviceId(from_device_id)],
        changedAt = changed_at,
        batch = batch,
        keepMine = when {
            !kept -> KeepMine.OFFERED
            choices.syncsSetting(setting_key) -> KeepMine.SYNCED_AGAIN
            else -> KeepMine.KEPT_HERE
        },
    )

    private fun now() = Clock.System.now().toEpochMilliseconds()
}
