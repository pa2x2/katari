package eu.kanade.tachiyomi.data.backup.preferences

import mihon.core.common.platform.PlatformFacility
import tachiyomi.core.common.preference.PreferenceStore

/**
 * The preferences a backup holds, read from and restored into the stores the app reads them from. Android keeps every
 * profile's preferences in the shared store's file under keys naming the profile; desktop keeps the shared store and
 * each profile's stores in files of their own.
 */
@PlatformFacility
interface PreferenceBackupStores {
    /** The preferences all profiles share, by key, without any one profile's. */
    fun sharedPreferences(): Map<String, *>

    /** [profileId]'s preferences, by the keys its preference classes read them by. */
    fun profilePreferences(profileId: Long): Map<String, *>

    /** The store [profileId]'s preference classes read the preference [key] from. */
    fun profileStoreFor(profileId: Long, key: String): PreferenceStore
}
