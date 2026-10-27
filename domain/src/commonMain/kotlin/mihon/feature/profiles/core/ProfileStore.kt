package mihon.feature.profiles.core

import kotlinx.coroutines.flow.Flow
import tachiyomi.core.common.preference.PreferenceStore

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
     * Removes everything [profileId] keeps in its profile, app state and private stores. Profile IDs are reused once
     * the highest one is deleted, so a new profile is cleared too before it is set up.
     */
    fun deleteProfileState(profileId: Long)
}
