package app.katari.sync.client.preference

import app.katari.profiles.core.ProfileStore
import dev.zacsweers.metro.Inject
import tachiyomi.core.common.preference.Preference

/** Sync's own options (D050), the device's alone like every setting about how this device syncs. */
@Inject
class SyncPreferences(profileStore: ProfileStore) {
    private val store = profileStore.basePreferenceStore()

    /** Whether a first download, and later covers and fonts, may use a metered network; records always may. */
    val useMobileData: Preference<Boolean> = store.getBoolean("sync_use_mobile_data", false)

    /** Whether the device syncs on a schedule while the app is closed. */
    val syncInBackground: Preference<Boolean> = store.getBoolean("sync_in_background", true)

    /**
     * The name the person gave this device for its account's device list, blank for the platform's name. App state,
     * so no backup carries it to another device.
     */
    val deviceName: Preference<String> = store.getString(Preference.appStateKey("sync_device_name"))
}
