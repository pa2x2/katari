package app.katari.sync.client.part

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.account.DeviceDirectory
import app.katari.sync.client.apply.SyncWriting
import app.katari.sync.client.kind.devicechoice.DeviceChoicesKind
import app.katari.sync.client.kind.devicechoice.DeviceChoicesPayload
import app.katari.sync.client.kind.devicechoice.PublishedChoices
import app.katari.sync.client.payload.Stamped
import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.device.LinkedDevice
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import tachiyomi.data.DatabaseHandler

/** This device's choices told to the account, and what the account's other devices told it of theirs. */
@Inject
class DeviceChoices(
    private val handler: DatabaseHandler,
    private val writing: SyncWriting,
    private val preferences: SyncPreferences,
    private val directory: DeviceDirectory,
) {
    /**
     * Tells the account this device's choices when its record doesn't hold them: on its first sync with a version that
     * tells them, and after each change, whoever made it. A sync pass calls it before pushing.
     */
    suspend fun publish() = writing.write { session ->
        val device = DeviceId(sync_stateQueries.get().awaitAsOne().device_id ?: return@write)
        val key = DeviceChoicesKind.key(device)
        val choices = PublishedChoices.of(preferences.choices())
        val told = sync_recordsQueries.getByKey(key).awaitAsOneOrNull()
            ?.takeIf { !it.deleted && DeviceChoicesKind.reads(it.kind_version.toInt()) }
            ?.let { DeviceChoicesKind.decode(it.payload).choices.value }
        if (told == choices) return@write
        session.change(DeviceChoicesKind, key, DeviceChoicesPayload(device, Stamped(choices, session.newEdit())))
    }

    /** The account's other devices in its order, and each change to them or to what they told of their choices. */
    fun others(): Flow<List<OtherDeviceChoices>> = combine(
        directory.devices(),
        handler.subscribeToList { sync_recordsQueries.byKind(DeviceChoicesKind.name) },
    ) { devices, records ->
        val thisDevice = handler.await { sync_stateQueries.get().awaitAsOne().device_id }
        val told = records
            .filter { !it.deleted && DeviceChoicesKind.reads(it.kind_version.toInt()) }
            .map { DeviceChoicesKind.decode(it.payload) }
            .associate { it.device to it.choices.value.choiceSet() }
        devices.filter { it.id.value != thisDevice }.map { OtherDeviceChoices(it, told[it.id]) }
    }.distinctUntilChanged()
}

/**
 * Another device of the account with its [choices]; null until it tells them, which a version before this one never
 * does.
 */
data class OtherDeviceChoices(val device: LinkedDevice, val choices: ChoiceSet?)
