package app.katari.sync.client.setting.read

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import mihon.feature.profiles.core.ProfileStore
import mihon.feature.profiles.sourcepreferences.SourcePreferenceFiles
import tachiyomi.core.common.preference.ProfilePreferenceOwnerRegistry

/** Reads the stores the app's preference classes and the sources read; which keys belong to a profile, from [owners]. */
@Inject
@ContributesBinding(AppScope::class)
class StoreSettingReads(
    private val profileStore: ProfileStore,
    private val sourceFiles: SourcePreferenceFiles,
    private val owners: ProfilePreferenceOwnerRegistry,
) : SettingReads {
    override fun profileValue(profileId: Long, key: String): Any? = profileStore.profileStore(profileId).getAll()[key]

    override fun sharedValue(key: String): Any? = profileStore.basePreferenceStore().getAll()[key]

    override fun sourceValue(profileId: Long, sourceId: Long, key: String): Any? =
        sourceFiles.open(profileId, sourceId).all[key]

    override fun isProfileKey(key: String): Boolean = key in owners.ownership(existingKeys = setOf(key)).profileKeys
}
