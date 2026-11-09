package app.katari.sync.client.preference

import app.katari.profiles.core.ProfileStore
import app.katari.sync.client.part.ChoiceSet
import app.katari.sync.client.part.DataGroup
import app.katari.sync.client.setting.scope.PreferenceScope
import app.katari.sync.client.setting.scope.SettingsArea
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import tachiyomi.core.common.preference.Preference

/** Sync's own options (D050), the device's alone like every setting about how this device syncs. */
@Inject
class SyncPreferences(profileStore: ProfileStore) {
    private val store = profileStore.basePreferenceStore()

    /** Whether a first download, and later covers and fonts, may use a metered network; records always may. */
    val useMobileData: Preference<Boolean> = store.getBoolean("sync_use_mobile_data", false)

    /** Whether the device syncs on a schedule while the app is closed. */
    val syncInBackground: Preference<Boolean> = store.getBoolean("sync_in_background", true)

    /** Minutes between background syncs; one of [BACKGROUND_INTERVALS]. */
    val backgroundInterval: Preference<Int> = store.getInt("sync_background_interval", 30)

    /** Whether syncing is paused here: edits are still recorded, and go once it resumes. */
    val paused: Preference<Boolean> = store.getBoolean("sync_paused", false)

    /**
     * Whether changes, this device's and the account's, sync on mobile data; when not, they wait for Wi-Fi, and
     * opening a title still fetches its progress on any network.
     */
    val changesOnMobileData: Preference<Boolean> = store.getBoolean("sync_changes_on_mobile_data", true)

    /**
     * The name the person gave this device for its account's device list, blank for the platform's name. App state,
     * so no backup carries it to another device.
     */
    val deviceName: Preference<String> = store.getString(Preference.appStateKey("sync_device_name"))

    // What this device syncs. App state, like its name: a backup restored on another device must not carry them there.
    val dataOff: Preference<Set<String>> = store.getStringSet(Preference.appStateKey("sync_data_off"))
    val areasOff: Preference<Set<String>> = store.getStringSet(Preference.appStateKey("sync_areas_off"))

    /** Settings kept out of an area this device syncs. */
    val settingsOff: Preference<Set<String>> = store.getStringSet(Preference.appStateKey("sync_settings_off"))

    /** Settings that stay on each device by default that this device syncs ([PreferenceScope.DEVICE_BY_DEFAULT]). */
    val settingsOn: Preference<Set<String>> = store.getStringSet(Preference.appStateKey("sync_settings_on"))

    /** The uuids of profiles this device doesn't sync: its own kept here, or the account's not on it. */
    val profilesOff: Preference<Set<String>> = store.getStringSet(Preference.appStateKey("sync_profiles_off"))

    /** The uuids of profiles made on another device that wait for the person to say whether they come here. */
    val profilesAsking: Preference<Set<String>> = store.getStringSet(Preference.appStateKey("sync_profiles_asking"))

    /** Whether a profile made on another device asks before it arrives here. */
    val askBeforeNewProfiles: Preference<Boolean> =
        store.getBoolean(Preference.appStateKey("sync_ask_before_new_profiles"), false)

    companion object {
        /** The intervals background syncs run at, in minutes; Android runs periodic work at least 15 minutes apart. */
        val BACKGROUND_INTERVALS = listOf(15, 30, 60, 180, 360, 720, 1440)
    }

    /** [choices] as they change. */
    fun choicesChanges(): Flow<ChoiceSet> = combine(
        listOf(
            dataOff.changes(),
            areasOff.changes(),
            settingsOff.changes(),
            settingsOn.changes(),
            profilesOff.changes(),
            profilesAsking.changes(),
            askBeforeNewProfiles.changes(),
        ),
    ) { choices() }.distinctUntilChanged()

    fun choices(): ChoiceSet = ChoiceSet(
        dataOff = dataOff.get().enumsOf(DataGroup.entries),
        areasOff = areasOff.get().enumsOf(SettingsArea.entries),
        settingsOff = settingsOff.get(),
        settingsOn = settingsOn.get(),
        profilesOff = profilesOff.get() + profilesAsking.get(),
        askBeforeNewProfiles = askBeforeNewProfiles.get(),
    )
}

// A name a later version wrote and this one doesn't know chooses nothing here
private fun <E : Enum<E>> Set<String>.enumsOf(entries: List<E>): Set<E> =
    mapNotNullTo(HashSet()) { name -> entries.firstOrNull { it.name == name } }
