package mihon.desktop.backup

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.preferences.PreferenceBackupStores
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/**
 * Desktop keeps each profile's stores in files of their own. A profile's preference classes read its private
 * preferences, the tracker accounts, from its private store and all others from its profile store, so a backup reads
 * both and a restore puts each preference back into the one it is read from. App-state preferences aren't backed up.
 */
@Inject
class DesktopPreferenceBackupStores(private val profileStore: ProfileStore) : PreferenceBackupStores {
    override fun sharedPreferences(): Map<String, *> = profileStore.basePreferenceStore().getAll()

    override fun profilePreferences(profileId: Long): Map<String, *> =
        profileStore.profileStore(profileId).getAll() + profileStore.privateStore(profileId).getAll()

    override fun profileStoreFor(profileId: Long, key: String): PreferenceStore = if (Preference.isPrivate(key)) {
        profileStore.privateStore(profileId)
    } else {
        profileStore.profileStore(profileId)
    }
}
