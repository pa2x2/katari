package mihon.feature.profiles.core

import android.content.SharedPreferences
import androidx.core.content.edit
import tachiyomi.core.common.preference.Preference

class ProfilePreferenceMigration(
    private val sharedPreferences: SharedPreferences,
) {
    fun migrateLegacyPreferenceKeys(
        profileId: Long,
        profileKeys: Set<String>,
        appStateKeys: Set<String> = emptySet(),
        privateKeys: Set<String> = emptySet(),
    ) {
        sharedPreferences.edit(commit = true) {
            profileKeys.forEach { key ->
                migrateKey(key, ProfileAwarePreferenceStore.Namespace.namespacedKey(key, profileId), this)
            }
            appStateKeys.forEach { key ->
                val sourceKey = Preference.appStateKey(key)
                migrateKey(
                    sourceKey,
                    ProfileAwarePreferenceStore.Namespace.namespacedKey(sourceKey, profileId),
                    this,
                )
            }
            privateKeys.forEach { key ->
                val sourceKey = Preference.privateKey(key)
                migrateKey(
                    sourceKey,
                    ProfileAwarePreferenceStore.Namespace.namespacedKey(sourceKey, profileId),
                    this,
                )
            }
        }
    }

    fun copyLegacyPreferenceKeysToProfiles(
        profileIds: Iterable<Long>,
        profileKeys: Set<String>,
        appStateKeys: Set<String> = emptySet(),
        privateKeys: Set<String> = emptySet(),
    ) {
        sharedPreferences.edit(commit = true) {
            profileIds.forEach { profileId ->
                profileKeys.forEach { key ->
                    migrateKey(key, ProfileAwarePreferenceStore.Namespace.namespacedKey(key, profileId), this)
                }
                appStateKeys.forEach { key ->
                    val sourceKey = Preference.appStateKey(key)
                    migrateKey(
                        sourceKey,
                        ProfileAwarePreferenceStore.Namespace.namespacedKey(sourceKey, profileId),
                        this,
                    )
                }
                privateKeys.forEach { key ->
                    val sourceKey = Preference.privateKey(key)
                    migrateKey(
                        sourceKey,
                        ProfileAwarePreferenceStore.Namespace.namespacedKey(sourceKey, profileId),
                        this,
                    )
                }
            }
        }
    }

    fun cleanupLegacyPreferenceKeys(
        profileId: Long,
        profileKeys: Set<String>,
        appStateKeys: Set<String> = emptySet(),
        privateKeys: Set<String> = emptySet(),
    ) {
        sharedPreferences.edit(commit = true) {
            profileKeys.forEach { key ->
                cleanupKey(key, ProfileAwarePreferenceStore.Namespace.namespacedKey(key, profileId), this)
            }
            appStateKeys.forEach { key ->
                val sourceKey = Preference.appStateKey(key)
                cleanupKey(
                    sourceKey,
                    ProfileAwarePreferenceStore.Namespace.namespacedKey(sourceKey, profileId),
                    this,
                )
            }
            privateKeys.forEach { key ->
                val sourceKey = Preference.privateKey(key)
                cleanupKey(
                    sourceKey,
                    ProfileAwarePreferenceStore.Namespace.namespacedKey(sourceKey, profileId),
                    this,
                )
            }
        }
    }

    private fun migrateKey(
        sourceKey: String,
        targetKey: String,
        editor: SharedPreferences.Editor,
    ) {
        if (!sharedPreferences.contains(sourceKey)) return
        if (sharedPreferences.contains(targetKey)) return
        when (val value = sharedPreferences.all[sourceKey]) {
            is String -> editor.putString(targetKey, value)
            is Int -> editor.putInt(targetKey, value)
            is Long -> editor.putLong(targetKey, value)
            is Float -> editor.putFloat(targetKey, value)
            is Boolean -> editor.putBoolean(targetKey, value)
            is Set<*> -> editor.putStringSet(targetKey, value.filterIsInstance<String>().toSet())
        }
    }

    private fun cleanupKey(
        sourceKey: String,
        targetKey: String,
        editor: SharedPreferences.Editor,
    ) {
        if (!sharedPreferences.contains(sourceKey)) return
        if (!sharedPreferences.contains(targetKey)) return
        editor.remove(sourceKey)
    }
}
