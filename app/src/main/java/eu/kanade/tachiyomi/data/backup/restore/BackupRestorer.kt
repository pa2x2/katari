package eu.kanade.tachiyomi.data.backup.restore

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.backup.BackupDecoder
import eu.kanade.tachiyomi.data.backup.BackupNotifier
import eu.kanade.tachiyomi.data.backup.create.BackupCreateJob
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.BackupExtensionStore
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import eu.kanade.tachiyomi.data.backup.restore.restorers.CategoriesRestorer
import eu.kanade.tachiyomi.data.backup.restore.restorers.EntryRestorer
import eu.kanade.tachiyomi.data.backup.restore.restorers.ExtensionStoreRestorer
import eu.kanade.tachiyomi.data.backup.restore.restorers.PreferenceRestorer
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.util.system.createFileInCacheDir
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.entry.interactions.EntryDownloadInteraction
import mihon.feature.profiles.core.ProfileConstants
import mihon.feature.profiles.core.ProfileDatabase
import mihon.feature.profiles.core.ProfileManager
import mihon.feature.profiles.core.ProfileScopedBackup
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.Database
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

@OptIn(ExperimentalAtomicApi::class)
class BackupRestorer(
    private val context: Context,
    private val notifier: BackupNotifier,
    private val isSync: Boolean,

    private val database: Database = Injekt.get(),
    private val categoriesRestorer: CategoriesRestorer = CategoriesRestorer(),
    private val preferenceRestorer: PreferenceRestorer = PreferenceRestorer(context),
    private val extensionStoreRestorer: ExtensionStoreRestorer = ExtensionStoreRestorer(),
    private val entryRestorer: EntryRestorer = EntryRestorer(),
    private val profileDatabase: ProfileDatabase = Injekt.get(),
    private val profileManager: ProfileManager = Injekt.get(),
    private val profileStore: ProfileStore = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val entryDownloadInteraction: EntryDownloadInteraction = Injekt.get(),
) {

    private var restoreAmount = 0
    private val restoreProgress = AtomicInt(0)
    private val errors = CopyOnWriteArrayList<Pair<Date, String>>()

    /**
     * Mapping of source ID to source name from backup data
     */
    private var sourceMapping: Map<Long, String> = emptyMap()

    suspend fun restore(uri: Uri, options: RestoreOptions) {
        val startTime = System.currentTimeMillis()

        restoreFromFile(uri, options)

        // Invalidate download cache to ensure UI reflects any restored downloads
        if (options.libraryEntries) {
            try {
                entryDownloadInteraction.invalidateCaches()
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Failed to invalidate download cache after restore" }
            }
        }

        val time = System.currentTimeMillis() - startTime

        val logFile = writeErrorLog()

        notifier.showRestoreComplete(
            time,
            errors.size,
            logFile.parent,
            logFile.name,
            isSync,
        )
    }

    private suspend fun restoreFromFile(uri: Uri, options: RestoreOptions) {
        val backup = BackupDecoder(context).decode(uri)

        // Store source mapping for error messages
        val backupMaps = backup.backupSources
        sourceMapping = backupMaps.associate { it.sourceId to it.name }

        if (backup.backupProfiles.isNotEmpty()) {
            coroutineScope {
                restoreFromProfilesBackup(
                    backupProfiles = backup.backupProfiles,
                    activeProfileUuid = backup.activeProfileUuid,
                    backupPreferences = backup.backupPreferences,
                    backupExtensionStores = backup.backupExtensionStores,
                    options = options,
                )
            }
            return
        }

        val entries = backup.allEntries()

        if (options.libraryEntries) {
            restoreAmount += entries.size
        }
        if (options.categories) {
            restoreAmount += 1
        }
        if (options.appSettings) {
            restoreAmount += 1
        }
        if (options.extensionStores) {
            restoreAmount += backup.backupExtensionStores.size
        }
        if (options.sourceSettings) {
            restoreAmount += 1
        }

        coroutineScope {
            if (options.categories) {
                restoreCategories(backup.backupCategories)
            }
            if (options.appSettings) {
                restoreAppPreferences(backup.backupPreferences, backup.backupCategories.takeIf { options.categories })
            }
            if (options.sourceSettings) {
                restoreSourcePreferences(backup.backupSourcePreferences)
            }
            if (options.libraryEntries) {
                restoreEntries(entries, if (options.categories) backup.backupCategories else emptyList())
            }
            if (options.extensionStores) {
                restoreExtensionStores(backup.backupExtensionStores)
            }

            // TODO: optionally trigger online library + tracker update
        }
    }

    private suspend fun CoroutineScope.restoreFromProfilesBackup(
        backupProfiles: List<ProfileScopedBackup>,
        activeProfileUuid: String?,
        backupPreferences: List<BackupPreference>,
        backupExtensionStores: List<BackupExtensionStore>,
        options: RestoreOptions,
    ) {
        val previousProfileId = profileManager.activeProfileId

        if (options.libraryEntries) {
            restoreAmount += backupProfiles.sumOf { it.allEntries().size }
        }
        if (options.categories) {
            restoreAmount += backupProfiles.size
        }
        if (options.appSettings) {
            restoreAmount += backupProfiles.size
            if (backupPreferences.isNotEmpty()) {
                restoreAmount += 1
            }
        }
        if (options.sourceSettings) {
            restoreAmount += backupProfiles.size
        }
        if (options.extensionStores) {
            restoreAmount += backupExtensionStores.size
        }

        for (profileBackup in backupProfiles) {
            val profile = upsertProfile(profileBackup)
            profileManager.setActiveProfile(profile.id, rescheduleJobs = false)

            if (options.categories) {
                categoriesRestorer(profileBackup.categories)
                val progress = restoreProgress.incrementAndFetch()
                notifier.showRestoreProgress(
                    "${profile.name}: ${context.stringResource(MR.strings.categories)}",
                    progress,
                    restoreAmount,
                    isSync,
                )
            }

            if (options.appSettings) {
                preferenceRestorer.restoreAppForProfile(
                    profileId = profile.id,
                    preferences = profileBackup.preferences,
                    backupCategories = profileBackup.categories.takeIf { options.categories },
                    includeGlobalRestore = profile.id == ProfileConstants.DEFAULT_PROFILE_ID,
                    scheduleJobs = false,
                )
                val progress = restoreProgress.incrementAndFetch()
                notifier.showRestoreProgress(
                    "${profile.name}: ${context.stringResource(MR.strings.app_settings)}",
                    progress,
                    restoreAmount,
                    isSync,
                )
            }

            if (options.sourceSettings) {
                preferenceRestorer.restoreSource(profile.id, profileBackup.sourcePreferences)
                val progress = restoreProgress.incrementAndFetch()
                notifier.showRestoreProgress(
                    "${profile.name}: ${context.stringResource(MR.strings.source_settings)}",
                    progress,
                    restoreAmount,
                    isSync,
                )
            }

            if (options.libraryEntries) {
                // EntryRestorer resolves profile-scoped data through the active profile,
                // so finish this bundle before switching to the next profile.
                restoreEntries(
                    profileBackup.allEntries(),
                    if (options.categories) profileBackup.categories else emptyList(),
                ).join()
            }
        }

        if (options.appSettings && backupPreferences.isNotEmpty()) {
            preferenceRestorer.restoreGlobalApp(
                preferences = backupPreferences,
                scheduleJobs = false,
            )
            val progress = restoreProgress.incrementAndFetch()
            notifier.showRestoreProgress(
                context.stringResource(MR.strings.app_settings),
                progress,
                restoreAmount,
                isSync,
            )
        }

        if (options.extensionStores) {
            backupExtensionStores.forEach {
                try {
                    extensionStoreRestorer(it)
                } catch (e: Exception) {
                    errors.add(Date() to "Error Adding Store: ${it.name} : ${e.message}")
                }

                val progress = restoreProgress.incrementAndFetch()
                notifier.showRestoreProgress(
                    context.stringResource(MR.strings.extensionStores),
                    progress,
                    restoreAmount,
                    isSync,
                )
            }
        }

        val targetProfile = activeProfileUuid
            ?.let { profileDatabase.getProfileByUuid(it) }
            ?: profileDatabase.getProfileById(previousProfileId)
        if (targetProfile != null) {
            profileManager.setActiveProfile(targetProfile.id, rescheduleJobs = false)
        }

        if (options.appSettings) {
            LibraryUpdateJob.setupTask(
                context = context,
                prefInterval = libraryPreferences.autoUpdateInterval.get(),
            )
            BackupCreateJob.setupTask(context)
        }
    }

    private suspend fun upsertProfile(bundle: ProfileScopedBackup): mihon.feature.profiles.core.Profile {
        val existing = profileDatabase.getProfileByUuid(bundle.profile.uuid)
        if (existing != null) {
            profileDatabase.updateProfile(
                id = existing.id,
                name = bundle.profile.name,
                colorSeed = bundle.profile.colorSeed,
                position = bundle.profile.position,
                requiresAuth = false,
                isArchived = bundle.profile.isArchived,
            )
            if (bundle.profile.requiresAuth) {
                SecurityPreferences(profileStore.profileStore(existing.id)).useAuthenticator.set(true)
            }
            return profileDatabase.getProfileById(existing.id) ?: existing
        }

        val id = profileDatabase.insertProfile(
            uuid = bundle.profile.uuid,
            name = bundle.profile.name,
            colorSeed = bundle.profile.colorSeed,
            position = bundle.profile.position,
            requiresAuth = false,
            isArchived = bundle.profile.isArchived,
        )
        if (bundle.profile.requiresAuth) {
            SecurityPreferences(profileStore.profileStore(id)).useAuthenticator.set(true)
        }
        return requireNotNull(profileDatabase.getProfileById(id))
    }

    private fun CoroutineScope.restoreCategories(backupCategories: List<BackupCategory>) = launch {
        ensureActive()
        categoriesRestorer(backupCategories)

        val progress = restoreProgress.incrementAndFetch()
        notifier.showRestoreProgress(
            context.stringResource(MR.strings.categories),
            progress,
            restoreAmount,
            isSync,
        )
    }

    private fun CoroutineScope.restoreEntries(
        backupEntries: List<BackupEntry>,
        backupCategories: List<BackupCategory>,
    ) = launch {
        entryRestorer.sortByNew(backupEntries)
            .chunked(100)
            .forEach { chunk ->
                database.transaction {
                    chunk.forEach {
                        ensureActive()

                        try {
                            entryRestorer.restore(
                                it,
                                backupCategories,
                            )
                        } catch (e: Exception) {
                            val sourceName = sourceMapping[it.source] ?: it.source.toString()
                            errors.add(Date() to "${it.title} [$sourceName]: ${e.message}")
                        }

                        restoreProgress.incrementAndFetch()
                    }
                }
                notifier.showRestoreProgress(chunk.last().title, restoreProgress.load(), restoreAmount, isSync)
            }

        entryRestorer.restorePendingMerges()
    }

    private fun CoroutineScope.restoreAppPreferences(
        preferences: List<BackupPreference>,
        categories: List<BackupCategory>?,
    ) = launch {
        ensureActive()
        preferenceRestorer.restoreApp(
            preferences,
            categories,
        )

        val progress = restoreProgress.incrementAndFetch()
        notifier.showRestoreProgress(
            context.stringResource(MR.strings.app_settings),
            progress,
            restoreAmount,
            isSync,
        )
    }

    private fun CoroutineScope.restoreSourcePreferences(preferences: List<BackupSourcePreferences>) = launch {
        ensureActive()
        preferenceRestorer.restoreSource(preferences)

        val progress = restoreProgress.incrementAndFetch()
        notifier.showRestoreProgress(
            context.stringResource(MR.strings.source_settings),
            progress,
            restoreAmount,
            isSync,
        )
    }

    private fun CoroutineScope.restoreExtensionStores(
        backupExtensionStores: List<BackupExtensionStore>,
    ) = launch {
        backupExtensionStores
            .chunked(100)
            .forEach { chunk ->
                database.transaction {
                    chunk.forEach {
                        ensureActive()

                        try {
                            extensionStoreRestorer(it)
                        } catch (e: Exception) {
                            errors.add(Date() to "Error Adding Repo: ${it.name} : ${e.message}")
                        }

                        restoreProgress.incrementAndFetch()
                    }
                }
                notifier.showRestoreProgress(
                    context.stringResource(MR.strings.extensionStores),
                    restoreProgress.load(),
                    restoreAmount,
                    isSync,
                )
            }
    }

    private fun writeErrorLog(): File {
        try {
            if (errors.isNotEmpty()) {
                val file = context.createFileInCacheDir("mihon_restore_error.txt")
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

                file.bufferedWriter().use { out ->
                    errors.forEach { (date, message) ->
                        out.write("[${sdf.format(date)}] $message\n")
                    }
                }
                return file
            }
        } catch (_: Exception) {
            // Empty
        }
        return File("")
    }
}
