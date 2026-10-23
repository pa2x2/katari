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
import mihon.entry.interactions.source.EntrySourceSettingsFeature
import mihon.feature.profiles.core.ProfileAwarePreferenceStore
import mihon.feature.profiles.core.ProfileStore
import mihon.feature.profiles.sourcepreferences.SourcePreferenceFiles
import mihon.feature.profiles.sourcepreferences.sourcePreferenceFileName
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

@Inject
class PreferenceBackupCreator(
    private val sourceSettingsFeature: EntrySourceSettingsFeature,
    private val preferenceStore: PreferenceStore,
    private val profileStore: ProfileStore,
    private val sourcePreferenceFiles: SourcePreferenceFiles,
) {

    fun createApp(includePrivatePreferences: Boolean): List<BackupPreference> {
        return preferenceStore.getAll().toBackupPreferences()
            .withPrivatePreferences(includePrivatePreferences)
    }

    suspend fun createSource(includePrivatePreferences: Boolean): List<BackupSourcePreferences> {
        return createSource(profileStore.currentProfileId, includePrivatePreferences)
    }

    fun createApp(profileId: Long, includePrivatePreferences: Boolean): List<BackupPreference> {
        return profileStore.profileStore(profileId)
            .getAll()
            .toBackupPreferences()
            .withPrivatePreferences(includePrivatePreferences)
    }

    suspend fun createSource(profileId: Long, includePrivatePreferences: Boolean): List<BackupSourcePreferences> {
        return sourceSettingsFeature.supportedSourceIds()
            .map { localKey ->
                val sourceId = sourcePreferenceFiles.sourceIdOf(localKey)
                BackupSourcePreferences(
                    sourceKey = sourcePreferenceFileName(profileId, sourceId),
                    prefs = sourcePreferenceFiles.open(profileId, sourceId).all.toBackupPreferences()
                        .withPrivatePreferences(includePrivatePreferences),
                    sourceId = localKey,
                )
            }
            .filter { it.prefs.isNotEmpty() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, *>.toBackupPreferences(): List<BackupPreference> {
        return this
            .filterKeys { !ProfileAwarePreferenceStore.Namespace.isNamespacedKey(it) }
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
