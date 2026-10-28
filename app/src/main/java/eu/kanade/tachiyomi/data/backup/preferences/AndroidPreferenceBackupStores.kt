package eu.kanade.tachiyomi.data.backup.preferences

import dev.zacsweers.metro.Inject
import mihon.feature.profiles.core.ProfileAwarePreferenceStore
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.core.common.preference.PreferenceStore

/**
 * Every profile's preferences live in the shared store's file under keys naming the profile, which the profile's store
 * reads them by without the name. Keys that still name a profile are left out of what is backed up.
 */
@Inject
class AndroidPreferenceBackupStores(
    private val preferenceStore: PreferenceStore,
    private val profileStore: ProfileStore,
) : PreferenceBackupStores {
    override fun sharedPreferences(): Map<String, *> =
        preferenceStore.getAll().filterKeys { !ProfileAwarePreferenceStore.Namespace.isNamespacedKey(it) }

    override fun profilePreferences(profileId: Long): Map<String, *> =
        profileStore.profileStore(profileId).getAll()
            .filterKeys { !ProfileAwarePreferenceStore.Namespace.isNamespacedKey(it) }

    override fun profileStoreFor(profileId: Long, key: String): PreferenceStore = profileStore.profileStore(profileId)
}
