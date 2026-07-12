package eu.kanade.tachiyomi.data.backup.create.creators

import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import eu.kanade.tachiyomi.data.backup.models.BooleanPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.FloatPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.LongPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringSetPreferenceValue
import eu.kanade.tachiyomi.source.sourcePreferences
import mihon.feature.profiles.core.ProfileAwarePreferenceStore
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.source.repository.SourceRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class PreferenceBackupCreator(
    private val sourceRepository: SourceRepository = Injekt.get(),
    private val preferenceStore: PreferenceStore = Injekt.get(),
    private val profileStore: ProfileStore = Injekt.get(),
) {

    fun createApp(includePrivatePreferences: Boolean): List<BackupPreference> {
        return preferenceStore.getAll().toBackupPreferences()
            .withPrivatePreferences(includePrivatePreferences)
    }

    fun createSource(includePrivatePreferences: Boolean): List<BackupSourcePreferences> {
        return sourceRepository.getConfigurableSourceIds()
            .map { sourceId ->
                val sourceKey = profileStore.sourcePreferenceKey(sourceId)
                BackupSourcePreferences(
                    sourceKey = sourceKey,
                    prefs = sourcePreferences(sourceKey).all.toBackupPreferences()
                        .withPrivatePreferences(includePrivatePreferences),
                    sourceId = sourceId,
                )
            }
            .filter { it.prefs.isNotEmpty() }
    }

    fun createApp(profileId: Long, includePrivatePreferences: Boolean): List<BackupPreference> {
        return profileStore.profileStore(profileId)
            .getAll()
            .toBackupPreferences()
            .withPrivatePreferences(includePrivatePreferences)
    }

    fun createSource(profileId: Long, includePrivatePreferences: Boolean): List<BackupSourcePreferences> {
        return sourceRepository.getConfigurableSourceIds()
            .map { sourceId ->
                val sourceKey = profileStore.sourcePreferenceKey(sourceId, profileId)
                BackupSourcePreferences(
                    sourceKey = sourceKey,
                    prefs = sourcePreferences(sourceKey).all.toBackupPreferences()
                        .withPrivatePreferences(includePrivatePreferences),
                    sourceId = sourceId,
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
