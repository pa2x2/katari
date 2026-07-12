package mihon.feature.profiles.core

import kotlinx.coroutines.flow.Flow
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.data.ActiveProfileProvider

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
    fun sourcePreferenceKey(sourceId: Long, profileId: Long = currentProfileId): String
}

interface ProfileAwareStore : ProfileStore, ActiveProfileProvider
