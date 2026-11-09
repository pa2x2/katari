package app.katari.sync.client.part

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.katari.sync.client.apply.ApplySession
import app.katari.sync.client.apply.SyncWriting
import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.firstsync.SettingsWinner
import app.katari.sync.client.kind.profile.ProfileKind
import app.katari.sync.client.outbox.recordEditsMadeNow
import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.run.SyncRuns
import app.katari.sync.client.setting.read.SettingReads
import app.katari.sync.client.setting.read.personLevelSettings
import app.katari.sync.client.setting.scope.PreferenceScope
import app.katari.sync.client.setting.scope.PreferenceScopes
import app.katari.sync.client.setting.scope.SettingsArea
import dev.zacsweers.metro.Inject
import tachiyomi.core.common.preference.Preference
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
) {
    suspend fun syncData(group: DataGroup, on: Boolean) = change {
        preferences.dataOff.include(group.name, !on)
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
        if (on && keep == SettingsWinner.DEVICE) handler.recordEditsMadeNow(settingsOf(area))
        change(before = { if (on && keep == SettingsWinner.ACCOUNT) it.keepAccountSettings(area) }) {
            preferences.areasOff.include(area.name, !on)
        }
    }

    /**
     * Keeps one setting of an area that syncs out of it, or brings it back, taking the account's value; a setting
     * that stays on each device by default is instead chosen to sync.
     */
    suspend fun syncSetting(key: String, on: Boolean) = change(before = { if (on) it.keepAccountSetting(key) }) {
        when (PreferenceScopes.markOf(key)?.scope) {
            PreferenceScope.PERSON -> preferences.settingsOff.include(key, !on)
            PreferenceScope.DEVICE_BY_DEFAULT -> preferences.settingsOn.include(key, on)
            PreferenceScope.DEVICE, null -> error("$key never syncs")
        }
    }

    private suspend fun settingsOf(area: SettingsArea): List<Pair<String, LocalRef>> {
        val choices = preferences.choices()
        val profiles = handler.await { sync_local_rowsQueries.profiles().awaitAsList() }
        return profiles.filter { choices.syncsProfile(it.uuid) }
            .flatMap { settings.personLevelSettings(it._id, it.uuid == ProfileKind.DEFAULT_UUID, area) }
    }

    private suspend fun change(before: suspend (ApplySession) -> Unit = {}, save: () -> Unit) {
        save()
        if (!connection.state().isLinked) return
        writing.write { session ->
            before(session)
            session.followChoices()
        }
        runs.request()
    }
}

private fun Preference<Set<String>>.include(value: String, included: Boolean) {
    set(if (included) get() + value else get() - value)
}
