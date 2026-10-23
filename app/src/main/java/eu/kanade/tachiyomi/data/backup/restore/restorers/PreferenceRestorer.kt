package eu.kanade.tachiyomi.data.backup.restore.restorers

import android.content.Context
import android.util.Log
import dev.zacsweers.metro.Inject
import eu.kanade.domain.source.service.LegacyNsfwSourceConversion
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
import eu.kanade.tachiyomi.data.library.LibraryUpdateSchedule
import mihon.feature.library.update.legacy.LegacyExcludedSourcesConversion
import mihon.feature.library.update.legacy.LegacyUpdateCategoriesConversion
import mihon.feature.profiles.core.ProfileStore
import mihon.feature.profiles.sourcepreferences.SourcePreferenceFiles
import mihon.feature.profiles.sourcepreferences.extensionPreferenceSourceId
import tachiyomi.core.common.preference.AndroidPreferenceStore
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.plusAssign
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository

@Inject
class PreferenceRestorer(
    private val context: Context,
    private val getCategories: GetCategories,
    private val preferenceStore: PreferenceStore,
    private val profileStore: ProfileStore,
    private val sourcePreferenceFiles: SourcePreferenceFiles,
    categoryRepository: CategoryRepository,
    updateRulesRepository: LibraryUpdateRulesRepository,
) {
    private val legacyUpdateCategoriesConversion =
        LegacyUpdateCategoriesConversion(categoryRepository, updateRulesRepository)

    suspend fun restoreGlobalApp(
        preferences: List<BackupPreference>,
        scheduleJobs: Boolean = true,
    ) {
        restorePreferences(toRestore = preferences, preferenceStore = preferenceStore)

        if (scheduleJobs) {
            LibraryUpdateSchedule.scheduleAll(context)
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
        val store = profileStore.profileStore(profileId)
        restorePreferences(
            toRestore = preferences,
            preferenceStore = store,
            backupCategoriesById = backupCategories?.associateBy { it.id.toString() }.orEmpty(),
            categoriesByName = if (backupCategories != null) {
                getCategories.awaitForProfile(profileId).associateBy { it.name }
            } else {
                emptyMap()
            },
        )
        legacyUpdateCategoriesConversion.convert(profileId, store)
        LegacyExcludedSourcesConversion.convert(store)
        LegacyNsfwSourceConversion.convert(store)

        if (includeGlobalRestore) {
            restorePreferences(toRestore = preferences, preferenceStore = preferenceStore)
        }

        if (scheduleJobs) {
            LibraryUpdateSchedule.schedule(context, profileId)
            BackupCreateJob.setupTask(context)
        }
    }

    suspend fun restoreSource(profileId: Long, preferences: List<BackupSourcePreferences>) {
        preferences.forEach {
            val sourceId = it.sourceId?.let { localKey -> sourcePreferenceFiles.sourceIdOf(localKey) }
                ?: backedUpSourceId(it.sourceKey)
                ?: return@forEach
            val sourcePrefs = AndroidPreferenceStore(context, sourcePreferenceFiles.open(profileId, sourceId))
            restorePreferences(it.prefs, sourcePrefs)
        }
    }

    // Backups without a source ID name the file the source used: Mihon's `source_<id>`, or `source_<profile>_<id>`
    private fun backedUpSourceId(fileName: String): Long? {
        return extensionPreferenceSourceId(fileName)
            ?: PROFILE_SOURCE_FILE.matchEntire(fileName)?.groupValues?.get(1)?.toLongOrNull()
    }

    private suspend fun restorePreferences(
        toRestore: List<BackupPreference>,
        preferenceStore: PreferenceStore,
        backupCategoriesById: Map<String, BackupCategory> = emptyMap(),
        categoriesByName: Map<String, Category> = emptyMap(),
    ) {
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

    private companion object {
        val PROFILE_SOURCE_FILE = Regex("""source_-?\d+_(-?\d+)""")
    }
}
