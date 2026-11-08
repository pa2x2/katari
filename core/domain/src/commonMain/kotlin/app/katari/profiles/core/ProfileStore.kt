package app.katari.profiles.core

import app.katari.core.common.platform.PlatformFacility
import kotlinx.coroutines.flow.Flow
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

@PlatformFacility
interface ProfileStore {
    val currentProfileId: Long
    val currentProfileIdFlow: Flow<Long>
    fun setCurrentProfileId(profileId: Long)
    fun basePreferenceStore(): PreferenceStore
    fun appStateStore(): PreferenceStore
    fun privateStore(): PreferenceStore
    fun profileStore(): PreferenceStore
    fun profileStore(profileId: Long): PreferenceStore
    fun appStateStore(profileId: Long): PreferenceStore
    fun privateStore(profileId: Long): PreferenceStore

    /**
     * Whether [preference] was taken from a profile's own store, the profile, app state or private one, rather than
     * from the base store every profile shares. Settings mark such preferences as the profile's.
     */
    fun isProfilePreference(preference: Preference<*>): Boolean

    /**
     * Calls [listener] with every write to the shared store or to any profile's own stores, from every writer, on the
     * writing thread once the new value can be read, until the returned handle is closed.
     */
    fun watchPreferenceWrites(listener: (PreferenceWrite) -> Unit): AutoCloseable

    /**
     * Removes everything [profileId] keeps in its profile, app state and private stores. Profile IDs are reused once
     * the highest one is deleted, so a new profile is cleared too before it is set up.
     */
    fun deleteProfileState(profileId: Long)
}
