package eu.kanade.tachiyomi.data.backup.create.creators

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import eu.kanade.tachiyomi.data.backup.models.BooleanPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.FloatPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.LongPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringSetPreferenceValue
import eu.kanade.tachiyomi.data.backup.preferences.PreferenceBackupStores
import eu.kanade.tachiyomi.data.backup.preferences.SourcePreferenceBackupStores
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.core.common.preference.Preference

@Inject
class PreferenceBackupCreator(
    private val preferenceStores: PreferenceBackupStores,
    private val sourcePreferenceStores: SourcePreferenceBackupStores,
    private val profileStore: ProfileStore,
) {

    fun createApp(includePrivatePreferences: Boolean): List<BackupPreference> {
        return preferenceStores.sharedPreferences().toBackupPreferences()
            .withPrivatePreferences(includePrivatePreferences)
    }

    suspend fun createSource(includePrivatePreferences: Boolean): List<BackupSourcePreferences> {
        return createSource(profileStore.currentProfileId, includePrivatePreferences)
    }

    fun createApp(profileId: Long, includePrivatePreferences: Boolean): List<BackupPreference> {
        return preferenceStores.profilePreferences(profileId)
            .toBackupPreferences()
            .withPrivatePreferences(includePrivatePreferences)
    }

    suspend fun createSource(profileId: Long, includePrivatePreferences: Boolean): List<BackupSourcePreferences> {
        return sourcePreferenceStores.sourceKeys()
            .map { localKey ->
                val snapshot = sourcePreferenceStores.read(profileId, localKey)
                BackupSourcePreferences(
                    sourceKey = snapshot.fileName,
                    prefs = snapshot.values.toBackupPreferences()
                        .withPrivatePreferences(includePrivatePreferences),
                    sourceId = localKey,
                )
            }
            .filter { it.prefs.isNotEmpty() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, *>.toBackupPreferences(): List<BackupPreference> {
        return this
            .filterKeys { !Preference.isAppState(it) }
            .mapNotNull { (key, value) ->
                when (value) {
                    is Int -> BackupPreference(key, IntPreferenceValue(value))
                    is Long -> BackupPreference(key, LongPreferenceValue(value))
                    is Float -> BackupPreference(key, FloatPreferenceValue(value))
                    is String -> BackupPreference(key, StringPreferenceValue(value))
                    is Boolean -> BackupPreference(key, BooleanPreferenceValue(value))
                    is Set<*> -> (value as? Set<String>)?.let {
                        BackupPreference(key, StringSetPreferenceValue(it))
                    }
                    else -> null
                }
            }
    }

    private fun List<BackupPreference>.withPrivatePreferences(include: Boolean) =
        if (include) {
            this
        } else {
            this.filter { !Preference.isPrivate(it.key) }
        }
}
