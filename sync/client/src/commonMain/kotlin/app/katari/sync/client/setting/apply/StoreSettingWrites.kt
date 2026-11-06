package app.katari.sync.client.setting.apply

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import mihon.feature.profiles.core.ProfileStore
import mihon.feature.profiles.sourcepreferences.SourcePreferenceFiles
import tachiyomi.core.common.preference.PreferenceStore

/** Writes the stores [app.katari.sync.client.setting.read.StoreSettingReads] reads. */
@Inject
@ContributesBinding(AppScope::class)
class StoreSettingWrites(
    private val profileStore: ProfileStore,
    private val sourceFiles: SourcePreferenceFiles,
) : SettingWrites {
    override fun writeProfileValue(profileId: Long, key: String, value: Any?) =
        profileStore.profileStore(profileId).write(key, value)

    override fun writeSharedValue(key: String, value: Any?) = profileStore.basePreferenceStore().write(key, value)

    override fun writeSourceValue(profileId: Long, sourceId: Long, key: String, value: Any?) {
        val editor = sourceFiles.open(profileId, sourceId).edit()
        when (value) {
            null -> editor.remove(key)
            is String -> editor.putString(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Float -> editor.putFloat(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Set<*> -> editor.putStringSet(key, value.mapTo(LinkedHashSet()) { it as String })
            else -> throw IllegalArgumentException("Preferences can't hold a ${value::class.simpleName}")
        }
        editor.commit()
    }

    override fun deleteProfile(profileId: Long) {
        profileStore.deleteProfileState(profileId)
        sourceFiles.deleteProfile(profileId)
    }

    private fun PreferenceStore.write(key: String, value: Any?) {
        when (value) {
            null -> getString(key).delete()
            is String -> getString(key).set(value)
            is Int -> getInt(key).set(value)
            is Long -> getLong(key).set(value)
            is Float -> getFloat(key).set(value)
            is Boolean -> getBoolean(key).set(value)
            is Set<*> -> getStringSet(key).set(value.mapTo(LinkedHashSet()) { it as String })
            else -> throw IllegalArgumentException("Preferences can't hold a ${value::class.simpleName}")
        }
    }
}
