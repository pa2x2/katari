package app.katari.sync.client.firstsync

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.katari.sync.client.apply.SyncWriting
import app.katari.sync.client.kind.profile.ProfileKind
import app.katari.sync.client.kind.setting.ProfileSettingKind
import app.katari.sync.client.kind.setting.SharedSettingKind
import app.katari.sync.client.kind.setting.SourceSettingKind
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.setting.read.SettingReads
import app.katari.sync.client.setting.scope.PreferenceScope
import app.katari.sync.client.setting.scope.PreferenceScopes
import app.katari.sync.protocol.record.RecordEnvelope
import dev.zacsweers.metro.Inject
import mihon.domain.profile.ProfileSeedPreferences
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.data.DatabaseHandler
import kotlin.time.Clock

/**
 * The merge side of a device's first sync (D018, D027, D047). Data is always merged; what the person decides is which
 * of this device's profiles are the account's, and whose settings those keep.
 *
 * Order, for a device joining an account, after capture starts:
 * 1. [hasOwnData]. A device without data skips the screen and calls [pair] with [proposal]'s pairings as they are,
 *    taking the account's settings. So does the first device of an empty account, whose profiles all pair with nothing.
 * 2. [proposal], with the account's profile records, then [pair] with the person's choices.
 * 3. The device's existing database rows are recorded, unstamped, so they merge with the account's rows (unit 8).
 * 4. The account's records are applied, which writes the account's settings over this device's in paired profiles.
 * 5. The device's existing preferences are recorded, unstamped: keys the account had now hold its values and change
 *    nothing, and keys only this device set reach the account.
 *
 * Settings a profile keeps from the device are recorded by [pair] as edits made then, so they win over the account's
 * in step 4 and on every other device. Device-level settings are never synced, so neither side's are replaced.
 */
@Inject
class FirstSync(
    private val handler: DatabaseHandler,
    private val writing: SyncWriting,
    private val settings: SettingReads,
    private val profileStore: ProfileStore,
) {
    /**
     * Whether this device holds the person's data: anything in its database beyond the untouched default profile, or
     * a person-level setting the app didn't write itself ([ProfileSeedPreferences]). A device that doesn't takes the
     * whole account without questions.
     */
    suspend fun hasOwnData(): Boolean {
        if (handler.await { sync_first_syncQueries.hasOwnRows().awaitAsOne() }) return true
        val profiles = localProfiles()
        return settings.sharedKeys().any { it.isPersonLevel() && !settings.isProfileKey(it) } ||
            profiles.any { profile ->
                val seeded = ProfileSeedPreferences.untouched(profileStore.profileStore(profile.id))
                settings.profileKeys(profile.id).any { it.isPersonLevel() && it !in seeded } ||
                    settings.sourceKeys(profile.id).isNotEmpty()
            }
    }

    /** This device's profiles, each paired by name with one of the account's [records] where one matches. */
    suspend fun proposal(records: List<RecordEnvelope>): List<ProfilePairing> {
        val account = records
            .filter { it.kind == ProfileKind.name && it.kindVersion == ProfileKind.version && !it.deleted }
            .map { ProfileKind.decode(it.payload) }
            .map { AccountProfile(it.uuid, it.name.value) }
        return pairByName(localProfiles(), account)
    }

    /**
     * Makes each profile in [choices] the account profile it was paired with, and records the settings of those that
     * keep the device's as edits made now. The default profile's choice covers the settings shared by all profiles.
     * A profile added as new keeps its own identity.
     */
    suspend fun pair(choices: List<ProfileChoice>) {
        val local = localProfiles().associateBy { it.id }
        val paired = choices.filter { it.accountUuid != null }
        require(paired.map { it.accountUuid }.toSet().size == paired.size) { "An account profile is paired twice" }
        paired.forEach { choice ->
            val uuid = checkNotNull(local[choice.profileId]) { "No profile ${choice.profileId}" }.uuid
            require((uuid == ProfileKind.DEFAULT_UUID) == (choice.accountUuid == ProfileKind.DEFAULT_UUID)) {
                "The default profile pairs only with the account's default"
            }
        }
        writing.write {
            paired.filter { local.getValue(it.profileId).uuid != it.accountUuid }.forEach { choice ->
                sync_applyQueries.setProfileUuid(uuid = checkNotNull(choice.accountUuid), profileId = choice.profileId)
            }
        }
        val kept = paired.filter { it.settings == SettingsWinner.DEVICE }
        val edits = kept.flatMap { choice ->
            val profile = local.getValue(choice.profileId)
            buildList {
                settings.profileKeys(profile.id).filter { it.isPersonLevel() }.forEach { key ->
                    add(ProfileSettingKind.name to ProfileSettingKind.ref(profile.id, key))
                }
                settings.sourceKeys(profile.id).forEach { (sourceId, keys) ->
                    keys.forEach { add(SourceSettingKind.name to SourceSettingKind.ref(profile.id, sourceId, it)) }
                }
                if (profile.uuid == ProfileKind.DEFAULT_UUID) {
                    settings.sharedKeys().filter { it.isPersonLevel() && !settings.isProfileKey(it) }.forEach { key ->
                        add(SharedSettingKind.name to SharedSettingKind.ref(key))
                    }
                }
            }
        }
        recordEdits(edits)
    }

    /** Records [edits] as the settings capture records a person's, outside sync's writes, which would drop them. */
    private suspend fun recordEdits(edits: List<Pair<String, LocalRef>>) {
        if (edits.isEmpty()) return
        val now = Clock.System.now().toEpochMilliseconds()
        handler.await(inTransaction = true) {
            edits.forEach { (kind, ref) ->
                sync_outboxQueries.recordEdit(kind = kind, localRef = ref.json, changedAt = now)
            }
        }
    }

    private suspend fun localProfiles(): List<LocalProfile> = handler.await {
        sync_local_rowsQueries.profiles().awaitAsList().map { LocalProfile(it._id, it.uuid, it.name) }
    }

    private fun String.isPersonLevel() = PreferenceScopes.of(this) == PreferenceScope.PERSON
}
