package eu.kanade.tachiyomi.data.backup.restore.restorers

import app.katari.library.display.legacy.LegacyItemsPerRowConversion
import app.katari.library.update.legacy.LegacyExcludedSourcesConversion
import app.katari.library.update.legacy.LegacyUpdateCategoriesConversion
import app.katari.profiles.core.ProfileStore
import dev.zacsweers.metro.Inject
import eu.kanade.domain.source.service.LegacyMigrationFlagsConversion
import eu.kanade.domain.source.service.LegacyNsfwSourceConversion
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
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
import logcat.LogPriority
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.plusAssign
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.backup.service.BackupCreateSchedule
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryUpdateSchedule
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository

@Inject
class PreferenceRestorer(
    private val getCategories: GetCategories,
    private val preferenceStore: PreferenceStore,
    private val profileStore: ProfileStore,
    private val preferenceStores: PreferenceBackupStores,
    private val sourcePreferenceStores: SourcePreferenceBackupStores,
    private val libraryUpdateSchedule: LibraryUpdateSchedule,
    private val backupCreateSchedule: BackupCreateSchedule,
    categoryRepository: CategoryRepository,
    updateRulesRepository: LibraryUpdateRulesRepository,
) {
    private val legacyUpdateCategoriesConversion =
        LegacyUpdateCategoriesConversion(categoryRepository, updateRulesRepository)

    suspend fun restoreGlobalApp(
        preferences: List<BackupPreference>,
        scheduleJobs: Boolean = true,
    ) {
        restorePreferences(toRestore = preferences, existing = preferenceStore.getAll()) { preferenceStore }

        if (scheduleJobs) {
            libraryUpdateSchedule.scheduleAll()
            backupCreateSchedule.setupTask()
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
            existing = preferenceStores.profilePreferences(profileId),
            backupCategoriesById = backupCategories?.associateBy { it.id.toString() }.orEmpty(),
            categoriesByName = if (backupCategories != null) {
                getCategories.awaitForProfile(profileId).associateBy { it.name }
            } else {
                emptyMap()
            },
        ) { key -> preferenceStores.profileStoreFor(profileId, key) }
        legacyUpdateCategoriesConversion.convert(profileId, store)
        LegacyExcludedSourcesConversion.convert(store)
        LegacyNsfwSourceConversion.convert(store)
        LegacyMigrationFlagsConversion.convert(store)
        LegacyItemsPerRowConversion.convert(store)

        if (includeGlobalRestore) {
            restorePreferences(toRestore = preferences, existing = preferenceStore.getAll()) { preferenceStore }
        }

        if (scheduleJobs) {
            libraryUpdateSchedule.schedule(profileId)
            backupCreateSchedule.setupTask()
        }
    }

    suspend fun restoreSource(profileId: Long, preferences: List<BackupSourcePreferences>) {
        preferences.forEach {
            val sourcePrefs = sourcePreferenceStores.storeFor(profileId, it.sourceId, it.sourceKey) ?: return@forEach
            restorePreferences(it.prefs, sourcePrefs.getAll()) { sourcePrefs }
        }
    }

    /**
     * [existing] are the preferences already set, whose types restored ones must match. [storeFor] gives the store
     * that holds a key.
     */
    private suspend fun restorePreferences(
        toRestore: List<BackupPreference>,
        existing: Map<String, *>,
        backupCategoriesById: Map<String, BackupCategory> = emptyMap(),
        categoriesByName: Map<String, Category> = emptyMap(),
        storeFor: (key: String) -> PreferenceStore,
    ) {
        toRestore.forEach { (key, value) ->
            val preferenceStore = storeFor(key)
            try {
                when (value) {
                    is IntPreferenceValue -> {
                        if (existing[key] is Int?) {
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
                        if (existing[key] is Long?) {
                            preferenceStore.getLong(key).set(value.value)
                        }
                    }
                    is FloatPreferenceValue -> {
                        if (existing[key] is Float?) {
                            preferenceStore.getFloat(key).set(value.value)
                        }
                    }
                    is StringPreferenceValue -> {
                        if (existing[key] is String?) {
                            preferenceStore.getString(key).set(value.value)
                        }
                    }
                    is BooleanPreferenceValue -> {
                        if (existing[key] is Boolean?) {
                            preferenceStore.getBoolean(key).set(value.value)
                        }
                    }
                    is StringSetPreferenceValue -> {
                        if (existing[key] is Set<*>?) {
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
                logcat(LogPriority.ERROR, e) { "Failed to restore preference <$key>" }
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
