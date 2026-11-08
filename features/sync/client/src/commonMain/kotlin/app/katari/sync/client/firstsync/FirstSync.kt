package app.katari.sync.client.firstsync

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.katari.domain.profile.ProfileSeedPreferences
import app.katari.profiles.core.ProfileStore
import app.katari.sync.client.apply.SyncWriting
import app.katari.sync.client.kind.profile.ProfileKind
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.setting.read.SettingReads
import app.katari.sync.protocol.record.RecordEnvelope
import dev.zacsweers.metro.Inject
import eu.kanade.domain.base.OnboardingPreferences
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
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
 * 3. The device's existing database rows are recorded, unstamped, so they merge with the account's rows
 *    ([ExistingRows]).
 * 4. The account's records are applied, which writes the account's settings over this device's in paired profiles.
 * 5. The device's existing preferences are recorded, unstamped: keys the account had now hold its values and change
 *    nothing, and keys only this device set reach the account ([ExistingSettings]).
 *
 * [FirstSyncSteps] runs these steps in this order.
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
     * a person-level setting the app didn't write itself ([ProfileSeedPreferences]) and the unfinished first-run guide
     * didn't ask for ([OnboardingPreferences.settingsChosenInGuide]). A device that doesn't takes the whole account
     * without questions.
     */
    suspend fun hasOwnData(): Boolean {
        val found = ownData() ?: return false
        // Which data asks the person is what tells a device that should have been fresh apart
        logcat(LogPriority.INFO) { "First sync: this device has data of its own ($found)" }
        return true
    }

    /** What makes this device count as holding the person's data, or null when nothing does. */
    private suspend fun ownData(): String? {
        if (handler.await { sync_first_syncQueries.hasOwnRows().awaitAsOne() }) return "database rows"
        settings.sharedKeys().firstOrNull { it.isPersonLevel() && !settings.isProfileKey(it) }
            ?.let { return "setting $it" }
        val chosenInGuide = OnboardingPreferences(profileStore.basePreferenceStore()).settingsChosenInGuide()
        localProfiles().forEach { profile ->
            val seeded = ProfileSeedPreferences.untouched(profileStore.profileStore(profile.id))
            settings.profileKeys(profile.id).firstOrNull { it.isPersonLevel() && it !in seeded && it !in chosenInGuide }
                ?.let { return "profile ${profile.id} setting $it" }
            settings.sourceKeys(profile.id).keys.firstOrNull()?.let {
                return "profile ${profile.id} source $it settings"
            }
        }
        return null
    }

    /**
     * Whether this device synced with the account before and kept the records of that connection, as one signing in
     * again after its server started over does (D066): it then resyncs them instead of recording its rows as new.
     */
    suspend fun keepsRecords(): Boolean = handler.await { sync_recordsQueries.hasSynced().awaitAsOne() }

    /** This device's profiles, each paired with the one of the account's [records] it is, or one of its name. */
    suspend fun proposal(records: List<RecordEnvelope>): List<ProfilePairing> {
        val account = records
            .filter { it.kind == ProfileKind.name && ProfileKind.reads(it.kindVersion) && !it.deleted }
            .map { ProfileKind.decode(it.payload) }
            .map { AccountProfile(it.uuid, it.name.value) }
        val librarySizes = handler.await {
            sync_first_syncQueries.librarySizes().awaitAsList().associate { it.profile_id to it.size }
        }
        return proposePairings(localProfiles(), account, librarySizes)
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
        val byIdentity = local.values.associateBy { it.uuid }
        paired.forEach { choice ->
            val uuid = checkNotNull(local[choice.profileId]) { "No profile ${choice.profileId}" }.uuid
            require((uuid == ProfileKind.DEFAULT_UUID) == (choice.accountUuid == ProfileKind.DEFAULT_UUID)) {
                "The default profile pairs only with the account's default"
            }
            // Two profiles of this device can't become one account profile: the other one already is it
            val holder = byIdentity[choice.accountUuid]
            require(holder == null || holder.id == choice.profileId) {
                "Profile ${holder?.id} already is the account profile paired with profile ${choice.profileId}"
            }
        }
        val keeps = keepsRecords()
        // Writing drains the edits not recorded yet first, under the identities they were made under
        writing.write {
            paired.filter { local.getValue(it.profileId).uuid != it.accountUuid }.forEach { choice ->
                sync_applyQueries.setProfileUuid(uuid = checkNotNull(choice.accountUuid), profileId = choice.profileId)
                // A kept record of the profile would bring its old identity back; its rows' records go when recorded
                val previous = local.getValue(choice.profileId).uuid
                if (keeps) sync_recordsQueries.deleteByLocalRef(ProfileKind.name, ProfileKind.ref(previous).json)
            }
        }
        val edits = paired.filter { it.settings == SettingsWinner.DEVICE }.flatMap { choice ->
            val profile = local.getValue(choice.profileId)
            settings.personLevelSettings(profile.id, isDefaultProfile = profile.uuid == ProfileKind.DEFAULT_UUID)
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
}
