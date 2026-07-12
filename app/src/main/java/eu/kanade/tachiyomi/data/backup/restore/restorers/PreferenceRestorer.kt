package eu.kanade.tachiyomi.data.backup.restore.restorers

import android.content.Context
import android.util.Log
import eu.kanade.tachiyomi.data.backup.create.BackupCreateJob
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import eu.kanade.tachiyomi.data.backup.models.BooleanPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.FloatPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.LongPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringSetPreferenceValue
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.source.sourcePreferences
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.core.common.preference.AndroidPreferenceStore
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.plusAssign
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.library.service.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class PreferenceRestorer(
    private val context: Context,
    private val getCategories: GetCategories = Injekt.get(),
    private val preferenceStore: PreferenceStore = Injekt.get(),
    private val profileStore: ProfileStore = Injekt.get(),
) {
    suspend fun restoreApp(
        preferences: List<BackupPreference>,
        backupCategories: List<BackupCategory>?,
        scheduleJobs: Boolean = true,
    ) {
        restoreAppForProfile(
            profileId = profileStore.currentProfileId,
            preferences = preferences,
            backupCategories = backupCategories,
            includeGlobalRestore = true,
            scheduleJobs = scheduleJobs,
        )
    }

    suspend fun restoreGlobalApp(
        preferences: List<BackupPreference>,
        scheduleJobs: Boolean = true,
    ) {
        restorePreferences(
            toRestore = preferences,
            preferenceStore = preferenceStore,
            backupCategories = null,
        )

        if (scheduleJobs) {
            LibraryUpdateJob.setupTask(context)
            BackupCreateJob.setupTask(context)
        }
    }

    suspend fun restoreAppForProfile(
        profileId: Long,
        preferences: List<BackupPreference>,
        backupCategories: List<BackupCategory>?,
        includeGlobalRestore: Boolean,
        scheduleJobs: Boolean,
    ) {
        restorePreferences(
            toRestore = preferences,
            preferenceStore = profileStore.profileStore(profileId),
            backupCategories = backupCategories,
        )

        if (includeGlobalRestore) {
            restorePreferences(
                toRestore = preferences,
                preferenceStore = preferenceStore,
                backupCategories = null,
            )
        }

        if (scheduleJobs) {
            LibraryUpdateJob.setupTask(context)
            BackupCreateJob.setupTask(context)
        }
    }

    suspend fun restoreSource(preferences: List<BackupSourcePreferences>) {
        preferences.forEach {
            val targetKey = it.sourceId
                ?.let(profileStore::sourcePreferenceKey)
                ?: it.sourceKey.toProfileAwareSourceKey(profileStore.currentProfileId)
            val sourcePrefs = AndroidPreferenceStore(context, sourcePreferences(targetKey))
            restorePreferences(it.prefs, sourcePrefs)
        }
    }

    suspend fun restoreSource(profileId: Long, preferences: List<BackupSourcePreferences>) {
        preferences.forEach {
            val targetKey = it.sourceId
                ?.let { sourceId -> profileStore.sourcePreferenceKey(sourceId, profileId) }
                ?: it.sourceKey.toProfileAwareSourceKey(profileId)
            val sourcePrefs = AndroidPreferenceStore(context, sourcePreferences(targetKey))
            restorePreferences(it.prefs, sourcePrefs)
        }
    }

    private fun String.toProfileAwareSourceKey(profileId: Long): String {
        val sourceId = removePrefix("source_")
            .takeIf { startsWith("source_") }
            ?.toLongOrNull()
            ?: return this
        return profileStore.sourcePreferenceKey(sourceId, profileId)
    }

    private suspend fun restorePreferences(
        toRestore: List<BackupPreference>,
        preferenceStore: PreferenceStore,
        backupCategories: List<BackupCategory>? = null,
    ) {
        val allCategories = if (backupCategories != null) getCategories.await() else emptyList()
        val categoriesByName = allCategories.associateBy { it.name }
        val backupCategoriesById = backupCategories?.associateBy { it.id.toString() }.orEmpty()
        val prefs = preferenceStore.getAll()
        toRestore.forEach { (key, value) ->
            try {
                when (value) {
                    is IntPreferenceValue -> {
                        if (prefs[key] is Int?) {
                            val newValue = if (key == LibraryPreferences.DEFAULT_CATEGORY_PREF_KEY) {
                                backupCategoriesById[value.value.toString()]
                                    ?.let { categoriesByName[it.name]?.id?.toInt() }
                            } else {
                                value.value
                            }

                            newValue?.let { preferenceStore.getInt(key).set(it) }
                        }
                    }
                    is LongPreferenceValue -> {
                        if (prefs[key] is Long?) {
                            preferenceStore.getLong(key).set(value.value)
                        }
                    }
                    is FloatPreferenceValue -> {
                        if (prefs[key] is Float?) {
                            preferenceStore.getFloat(key).set(value.value)
                        }
                    }
                    is StringPreferenceValue -> {
                        if (prefs[key] is String?) {
                            preferenceStore.getString(key).set(value.value)
                        }
                    }
                    is BooleanPreferenceValue -> {
                        if (prefs[key] is Boolean?) {
                            preferenceStore.getBoolean(key).set(value.value)
                        }
                    }
                    is StringSetPreferenceValue -> {
                        if (prefs[key] is Set<*>?) {
                            val restored = restoreCategoriesPreference(
                                key,
                                value.value,
                                preferenceStore,
                                backupCategoriesById,
                                categoriesByName,
                            )
                            if (!restored) preferenceStore.getStringSet(key).set(value.value)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PreferenceRestorer", "Failed to restore preference <$key>", e)
            }
        }
    }

    private fun restoreCategoriesPreference(
        key: String,
        value: Set<String>,
        preferenceStore: PreferenceStore,
        backupCategoriesById: Map<String, BackupCategory>,
        categoriesByName: Map<String, Category>,
    ): Boolean {
        val categoryPreferences = LibraryPreferences.categoryPreferenceKeys + DownloadPreferences.categoryPreferenceKeys
        if (key !in categoryPreferences) return false

        val ids = value.mapNotNull {
            backupCategoriesById[it]?.name?.let { name ->
                categoriesByName[name]?.id?.toString()
            }
        }

        if (ids.isNotEmpty()) {
            preferenceStore.getStringSet(key) += ids
        }
        return true
    }
}
