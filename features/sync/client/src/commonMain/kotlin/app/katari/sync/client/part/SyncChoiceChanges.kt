package app.katari.sync.client.part

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.apply.ApplySession
import app.katari.sync.client.apply.SyncWriting
import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.file.capture.FileCapture
import app.katari.sync.client.file.local.dataGroup
import app.katari.sync.client.firstsync.SettingsWinner
import app.katari.sync.client.kind.profile.ProfileKind
import app.katari.sync.client.outbox.recordEditsMadeNow
import app.katari.sync.client.outbox.recordUnstamped
import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.run.SyncRuns
import app.katari.sync.client.setting.read.SettingReads
import app.katari.sync.client.setting.read.settingRecords
import app.katari.sync.client.setting.scope.PreferenceScope
import app.katari.sync.client.setting.scope.PreferenceScopes
import app.katari.sync.client.setting.scope.SettingsArea
import app.katari.sync.protocol.file.FileCategory
import dev.zacsweers.metro.Inject
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.minusAssign
import tachiyomi.core.common.preference.plusAssign
import tachiyomi.data.DatabaseHandler

/**
 * Changes what this device syncs. A choice is saved first, so every drain and apply after it follows it; then the
 * records here are brought in line with it ([ApplySession.followChoices]) and a sync run pushes what was let go. A
 * device that isn't linked only saves the choice, which its first sync follows.
 */
@Inject
class SyncChoiceChanges(
    private val handler: DatabaseHandler,
    private val preferences: SyncPreferences,
    private val writing: SyncWriting,
    private val settings: SettingReads,
    private val connection: SyncConnection,
    private val runs: SyncRuns,
    private val fileCapture: FileCapture,
) {
    /**
     * Turns syncing [group] on or off. Turned on, the files of a group of files that changed here meanwhile are
     * recorded as changed now ([FileCapture.rescan]).
     */
    suspend fun syncData(group: DataGroup, on: Boolean) {
        val files = FileCategory.entries.firstOrNull { it.dataGroup == group }
        change(then = { if (on && files != null) fileCapture.rescan(files) }) {
            preferences.dataOff.include(group.name, !on)
        }
    }

    /**
     * Whether the profile [uuid] left this device with its records stored, which switching it on writes here again. A
     * profile only this device had is here already, and the records of one it never had wait to be taken in.
     */
    private suspend fun leftThisDevice(uuid: String): Boolean = handler.await {
        sync_applyQueries.profileId(uuid).awaitAsOneOrNull() == null &&
            sync_recordsQueries.getByKey(ProfileKind.key(uuid)).awaitAsOneOrNull() != null
    }

    /**
     * Whether switching the profile [uuid] off takes it off this device: it is the account's, where it stays, rather
     * than one only this device has, which stays here unsynced.
     */
    suspend fun leavesThisDevice(uuid: String): Boolean = handler.await {
        sync_recordsQueries.getByKey(ProfileKind.key(uuid)).awaitAsOneOrNull()?.synced == true
    }

    /**
     * Turns syncing the profile [uuid] on or off, which also answers whether a profile made on another device comes
     * here. An account profile switched off leaves this device ([leavesThisDevice]), so it must not be the open one;
     * switched on again, it comes back as the account has it. Every device syncs its default profile.
     */
    suspend fun syncProfile(uuid: String, on: Boolean) {
        require(uuid != ProfileKind.DEFAULT_UUID) { "Every device syncs its default profile" }
        val leaves = !on && leavesThisDevice(uuid)
        // Asked before the choice is followed, which may bring the profile's own record in first
        val comesBack = on && leftThisDevice(uuid)
        change(
            before = { session ->
                if (leaves) session.removeProfile(uuid)
                if (on) session.admitProfile(uuid)
            },
            after = { if (comesBack) it.restoreProfile(uuid) },
        ) {
            preferences.profilesAsking -= uuid
            preferences.profilesOff.include(uuid, !on)
        }
    }

    /**
     * Whether turning [area] back on asks whose settings to keep: this device changed them while it didn't sync them.
     * Settings the device left alone meanwhile take the account's either way.
     */
    suspend fun asksWhoseSettings(area: SettingsArea): Boolean =
        connection.state().isLinked && writing.write { it.changedWhileOff(area) }

    /**
     * Turns syncing [area] on or off. Turned on, the settings the device changed meanwhile give way to the account's,
     * or, when [keep] is [SettingsWinner.DEVICE], are recorded as changed now, so they win over the account's here
     * and on every other device.
     */
    suspend fun syncArea(area: SettingsArea, on: Boolean, keep: SettingsWinner = SettingsWinner.ACCOUNT) {
        if (on && keep == SettingsWinner.DEVICE) {
            handler.recordEditsMadeNow(
                settingRecords(withSources = area == SettingsArea.EXTENSION_SETTINGS) {
                    PreferenceScopes.markOf(it)?.area == area
                },
            )
        }
        change(before = { if (on && keep == SettingsWinner.ACCOUNT) it.keepAccountSettings(area) }) {
            preferences.areasOff.include(area.name, !on)
        }
    }

    /**
     * Keeps one setting of an area that syncs out of it, or brings it back; a setting that stays on each device by
     * default is instead chosen to sync. Brought in, it takes the account's value, and the value here fills in only
     * where the account has none.
     */
    suspend fun syncSetting(key: String, on: Boolean) {
        val scope = PreferenceScopes.markOf(key)?.scope
        require(scope == PreferenceScope.PERSON || scope == PreferenceScope.DEVICE_BY_DEFAULT) { "$key never syncs" }
        if (on) handler.recordUnstamped(settingRecords(withSources = false) { it == key })
        change(before = { if (on) it.keepAccountSetting(key) }) {
            if (scope == PreferenceScope.PERSON) {
                preferences.settingsOff.include(key, !on)
            } else {
                preferences.settingsOn.include(key, on)
            }
        }
    }

    /** The settings of every profile this device syncs that [including] picks, as [settingRecords] are. */
    private suspend fun settingRecords(
        withSources: Boolean,
        including: (key: String) -> Boolean,
    ): List<Pair<String, LocalRef>> {
        val choices = preferences.choices()
        val profiles = handler.await { sync_local_rowsQueries.profiles().awaitAsList() }
        return profiles.filter { choices.syncsProfile(it.uuid) }.flatMap { profile ->
            settings.settingRecords(profile._id, profile.uuid == ProfileKind.DEFAULT_UUID, including, withSources)
        }
    }

    /**
     * Saves a choice with [save] and brings this device in line with it: [before] and [after] run in the same write as
     * [ApplySession.followChoices], and [then] once it is done, before the sync run it asks for.
     */
    private suspend fun change(
        before: suspend (ApplySession) -> Unit = {},
        after: suspend (ApplySession) -> Unit = {},
        then: suspend () -> Unit = {},
        save: () -> Unit,
    ) {
        save()
        if (!connection.state().isLinked) return
        writing.write { session ->
            before(session)
            session.followChoices()
            after(session)
        }
        then()
        runs.request()
    }
}

private fun Preference<Set<String>>.include(value: String, included: Boolean) {
    if (included) this += value else this -= value
}
