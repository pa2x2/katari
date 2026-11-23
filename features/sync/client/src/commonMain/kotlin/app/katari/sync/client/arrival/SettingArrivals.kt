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
 * The settings other devices changed that arrived here in the last 14 days, and Keep mine: it keeps the setting on
 * this device, then puts back the value it had. Kept here first, the value written back is held back like any edit of
 * a setting this device doesn't sync, and switching the setting on again later asks whose value it keeps.
 *
 * A setting of a menu ([ArrivedSetting.menu]) is kept with every other change to its menu not kept yet, whichever sync
 * brought it, as the menu's settings sync or stay as one.
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
    /** Every setting that arrived in the last 14 days, latest arrival first, as this device's choices stand. */
    fun changes(): Flow<List<ArrivedSetting>> = combine(
        handler.subscribeToList { sync_arrived_settingsQueries.all() },
        preferences.choicesChanges(),
    ) { rows, choices ->
        val since = now() - ArrivalNotes.KEPT_FOR.inWholeMilliseconds
        val names = directory.listed()
        rows.filter { it.batch >= since }.map { it.toArrived(names, choices) }
    }

    /** The settings each sync changes here from now on, as that sync is committed. */
    fun arrivals(): Flow<List<ArrivedSetting>> = batches.batches.map { batch ->
        val names = directory.listed()
        val choices = preferences.choices()
        handler.await { sync_arrived_settingsQueries.inBatch(batch).awaitAsList() }.map { it.toArrived(names, choices) }
    }

    /** Keeps [setting] on this device with the value it had, along with the rest of its menu's changes. */
    suspend fun keepMine(setting: ArrivedSetting) {
        val menu = setting.menu
        keep(unkept().filter { it._id == setting.id || (menu != null && it.arrivedMenu == menu) })
    }

    /** Keeps every setting [batch]'s sync changed here, as a notice of them offers, with the rest of their menus. */
    suspend fun keepMine(batch: Long) {
        val rows = unkept()
        val menus = rows.filter { it.batch == batch }.mapNotNull { it.arrivedMenu }.toSet()
        keep(rows.filter { it.batch == batch || it.arrivedMenu in menus })
    }

    private suspend fun unkept() = handler.await { sync_arrived_settingsQueries.unkept().awaitAsList() }

    /**
     * Keeps [rows]' settings here, then puts back their values. The choices' writes take in this device's edits first,
     * and a setting changed here since it arrived keeps that value ([app.katari.sync.client.outbox.OutboxDrain]). The
     * rows are marked kept before the values are written back, so that writing isn't taken for such an edit.
     */
    private suspend fun keep(rows: List<Sync_arrived_settings>) {
        if (rows.isEmpty()) return
        val menus = rows.mapNotNull { it.arrivedMenu }.toSet()
        menus.forEach { choices.syncMenu(it, on = false) }
        rows.filter { it.arrivedMenu == null }.map { it.setting_key }.toSet().forEach {
            choices.syncSetting(it, on = false)
        }
        val waiting = handler.await { sync_arrived_settingsQueries.byIds(rows.map { it._id }).awaitAsList() }
            .filter { !it.kept }
        handler.await { sync_arrived_settingsQueries.markKept(waiting.map { it._id }) }
        waiting.forEach { row ->
            val previous = decode(row.previous)
            val profileId = row.profile_id
            if (profileId == null) {
                settings.writeSharedValue(row.setting_key, previous)
            } else {
                settings.writeProfileValue(profileId, row.setting_key, previous)
            }
        }
    }

    private val Sync_arrived_settings.arrivedMenu
        get() = PreferenceScopes.markOf(setting_key)?.menu

    private fun Sync_arrived_settings.toArrived(names: Map<DeviceId, String>, choices: ChoiceSet) = ArrivedSetting(
        id = _id,
        profileId = profile_id,
        key = setting_key,
        from = names[DeviceId(from_device_id)],
        batch = batch,
        keepMine = when {
            !kept -> KeepMine.OFFERED
            choices.syncsSetting(setting_key) -> KeepMine.SYNCED_AGAIN
            else -> KeepMine.KEPT_HERE
        },
        change = arrived?.let { ArrivedChange(before = decode(previous), after = decode(it)) },
    )

    private fun decode(value: ByteArray) = ProtoBuf.decodeFromByteArray(SettingValue.serializer(), value).stored

    private fun now() = Clock.System.now().toEpochMilliseconds()
}
