package eu.kanade.tachiyomi.data.backup.restore

import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.backup.BackupDecoder
import eu.kanade.tachiyomi.data.backup.extensions.BackedUpFileExtensions
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupCategoryUpdateRules
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.BackupExtensionStore
import eu.kanade.tachiyomi.data.backup.models.BackupFileExtension
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import eu.kanade.tachiyomi.data.backup.restore.entries.EntryRestoreCoordinator
import eu.kanade.tachiyomi.data.backup.restore.restorers.CategoriesRestorer
import eu.kanade.tachiyomi.data.backup.restore.restorers.EntryRestorer
import eu.kanade.tachiyomi.data.backup.restore.restorers.ExtensionStoreRestorer
import eu.kanade.tachiyomi.data.backup.restore.restorers.PreferenceRestorer
import eu.kanade.tachiyomi.data.backup.restore.restorers.SourceNamesRestorer
import eu.kanade.tachiyomi.data.backup.sourcekeys.RestoredSourceKeys
import eu.kanade.tachiyomi.data.backup.storage.BackupWorkingFiles
import eu.kanade.tachiyomi.data.backup.stream.BackupSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.core.platform.PlatformContext
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreSession
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreSessionId
import mihon.feature.profiles.core.ProfileConstants
import mihon.feature.profiles.core.ProfileDatabase
import mihon.feature.profiles.core.ProfileScopedBackup
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.Database
import tachiyomi.domain.backup.model.RestoreOptions
import tachiyomi.domain.backup.service.BackupCreateSchedule
import tachiyomi.domain.library.service.LibraryUpdateSchedule
import tachiyomi.domain.source.identity.SourceIdentityRepository
import tachiyomi.domain.storage.DocumentUri
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.stringResource
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

@OptIn(ExperimentalAtomicApi::class)
@AssistedInject
class BackupRestorer(
    @Assisted private val reporter: BackupRestoreReporter,
    @Assisted private val isSync: Boolean,
    private val context: PlatformContext,
    private val workingFiles: BackupWorkingFiles,
    private val database: Database,
    private val backupDecoder: BackupDecoder,
    private val categoriesRestorer: CategoriesRestorer,
    private val preferenceRestorer: PreferenceRestorer,
    private val extensionStoreRestorer: ExtensionStoreRestorer,
    private val entryRestorer: EntryRestorer,
    private val sourceNamesRestorer: SourceNamesRestorer,
    private val sourceIdentities: SourceIdentityRepository,
    private val profileDatabase: ProfileDatabase,
    private val profileStore: ProfileStore,
    private val downloadMaintenance: EntryDownloadMaintenanceFeature,
    private val fileExtensions: BackedUpFileExtensions,
    private val libraryUpdateSchedule: LibraryUpdateSchedule,
    private val backupCreateSchedule: BackupCreateSchedule,
    private val syncedRestore: SyncedRestore,
) {
    @AssistedFactory
    fun interface Factory {
        fun create(reporter: BackupRestoreReporter, isSync: Boolean): BackupRestorer
    }

    private var restoreAmount = 0
    private val restoreProgress = AtomicInt(0)
    private val errors = CopyOnWriteArrayList<Pair<Date, String>>()

    /**
     * Mapping of source ID to source name from backup data
     */
    private var sourceMapping: Map<Long, String> = emptyMap()

    private val restoredProfileNames = CopyOnWriteArrayList<String>()

    suspend fun restore(uri: DocumentUri, options: RestoreOptions) {
        val startTime = System.currentTimeMillis()

        restoreFromFile(uri, options)

        // Invalidate download cache to ensure UI reflects any restored downloads
        if (options.libraryEntries) {
            try {
                downloadMaintenance.invalidateCaches()
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Failed to invalidate download cache after restore" }
            }
        }

        val time = System.currentTimeMillis() - startTime

        val logFile = writeErrorLog()

        reporter.onRestoreComplete(
            time,
            errors.size,
            logFile.parent,
            logFile.name,
            isSync,
            restoredProfileNames,
        )
    }

    private suspend fun restoreFromFile(uri: DocumentUri, options: RestoreOptions) {
        // Read once: a backup without profiles restores into the profile that was active when it started, even if
        // another one is switched to meanwhile.
        val targetProfileId = profileStore.currentProfileId
        val summary = backupDecoder.summarize(uri)
        if (options.rollBackAccount) {
            syncedRestore.rollingBackAccount(rolledBackProfiles(summary.metadata, targetProfileId)) {
                restoreSummarized(uri, options, summary, targetProfileId)
            }
        } else {
            syncedRestore.onThisDevice(summary.createdAt) { restoreSummarized(uri, options, summary, targetProfileId) }
        }
    }

    /** This device's profiles that [backup] holds, or [targetProfileId] for a backup without profiles. */
    private suspend fun rolledBackProfiles(backup: Backup, targetProfileId: Long): Set<Long> =
        if (backup.backupProfiles.isEmpty()) {
            setOf(targetProfileId)
        } else {
            backup.backupProfiles.mapNotNullTo(mutableSetOf()) { profileDatabase.getProfileByUuid(it.profile.uuid)?.id }
        }

    private suspend fun restoreSummarized(
        uri: DocumentUri,
        options: RestoreOptions,
        summary: BackupSummary,
        targetProfileId: Long,
    ) {
        val sourceKeys = RestoredSourceKeys.of(summary, sourceIdentities)
        val backup = sourceKeys.applyTo(summary.metadata)
        // Entries are read from the file again for each profile, so no more than a chunk of them is held at once
        val entries = { profileIndex: Int? ->
            backupDecoder.decodeEntries(uri, profileIndex).onEach { sourceKeys.applyTo(it) }
        }
        val entryRestore = EntryRestoreCoordinator(
            database,
            entryRestorer,
            EntryBackupRestoreSession(EntryBackupRestoreSessionId(UUID.randomUUID().toString()), sourceKeys),
        )

        // Store source mapping for error messages
        val backupMaps = backup.backupSources
        sourceMapping = backupMaps.associate { it.sourceId to it.name }
        if (options.libraryEntries) {
            val entrySources = summary.entrySources.mapTo(HashSet(), sourceKeys::localKey)
            sourceNamesRestorer(backupMaps.filter { it.sourceId in entrySources })
        }

        if (backup.backupProfiles.isNotEmpty()) {
            coroutineScope {
                restoreFromProfilesBackup(
                    entryRestore = entryRestore,
                    backupProfiles = backup.backupProfiles
                        .mapIndexed { index, profile ->
                            RestoredProfile(profile, summary.profileEntryCounts[index]) { entries(index) }
                        }
                        .filter { options.profileUuids?.contains(it.backup.profile.uuid) ?: true },
                    backupPreferences = backup.backupPreferences,
                    backupExtensionStores = backup.backupExtensionStores,
                    backupFileExtensions = backup.backupFileExtensions,
                    options = options,
                )
            }
            return
        }

        if (options.libraryEntries) {
            restoreAmount += summary.topLevelEntryCount
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
            // Default category preferences and entries are matched to categories that this job creates.
            val categoriesJob = if (options.categories) {
                restoreCategories(targetProfileId, backup.backupCategories, backup.backupDefaultCategoryUpdateRules)
            } else {
                null
            }
            if (options.appSettings) {
                restoreAppPreferences(
                    targetProfileId,
                    backup.backupPreferences,
                    backup.backupCategories.takeIf { options.categories },
                    categoriesJob,
                )
            }
            if (options.sourceSettings) {
                restoreSourcePreferences(targetProfileId, backup.backupSourcePreferences)
            }
            if (options.libraryEntries) {
                restoreEntries(
                    entryRestore,
                    targetProfileId,
                    entries(null),
                    if (options.categories) backup.backupCategories else emptyList(),
                    categoriesJob,
                )
            }
            if (options.extensionStores) {
                restoreExtensionStores(backup.backupExtensionStores).invokeOnCompletion { error ->
                    if (error == null) installFileExtensions(backup.backupFileExtensions)
                }
            }

            // TODO: optionally trigger online library update
        }
    }

    private suspend fun CoroutineScope.restoreFromProfilesBackup(
        entryRestore: EntryRestoreCoordinator,
        backupProfiles: List<RestoredProfile>,
        backupPreferences: List<BackupPreference>,
        backupExtensionStores: List<BackupExtensionStore>,
        backupFileExtensions: List<BackupFileExtension>,
        options: RestoreOptions,
    ) {
        if (options.libraryEntries) {
            restoreAmount += backupProfiles.sumOf { it.entryCount }
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

        for (restoredProfile in backupProfiles) {
            val profileBackup = restoredProfile.backup
            val profile = upsertProfile(profileBackup)

            if (options.categories) {
                categoriesRestorer(profile.id, profileBackup.categories, profileBackup.defaultCategoryUpdateRules)
                val progress = restoreProgress.incrementAndFetch()
                reporter.onRestoreProgress(
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
                reporter.onRestoreProgress(
                    "${profile.name}: ${context.stringResource(MR.strings.app_settings)}",
                    progress,
                    restoreAmount,
                    isSync,
                )
            }

            if (options.sourceSettings) {
                preferenceRestorer.restoreSource(profile.id, profileBackup.sourcePreferences)
                val progress = restoreProgress.incrementAndFetch()
                reporter.onRestoreProgress(
                    "${profile.name}: ${context.stringResource(MR.strings.source_settings)}",
                    progress,
                    restoreAmount,
                    isSync,
                )
            }

            if (options.libraryEntries) {
                restoreEntries(
                    entryRestore,
                    profile.id,
                    restoredProfile.entries(),
                    if (options.categories) profileBackup.categories else emptyList(),
                    categoriesJob = null,
                ).join()
            }
            restoredProfileNames += profile.name
        }

        if (options.appSettings && backupPreferences.isNotEmpty()) {
            preferenceRestorer.restoreGlobalApp(
                preferences = backupPreferences,
                scheduleJobs = false,
            )
            val progress = restoreProgress.incrementAndFetch()
            reporter.onRestoreProgress(
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
                reporter.onRestoreProgress(
                    context.stringResource(MR.strings.extensionStores),
                    progress,
                    restoreAmount,
                    isSync,
                )
            }
            installFileExtensions(backupFileExtensions)
        }

        if (options.appSettings) {
            libraryUpdateSchedule.scheduleAll()
            backupCreateSchedule.setupTask()
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

    private fun CoroutineScope.restoreCategories(
        profileId: Long,
        backupCategories: List<BackupCategory>,
        defaultCategoryUpdateRules: BackupCategoryUpdateRules?,
    ) = launch {
        ensureActive()
        categoriesRestorer(profileId, backupCategories, defaultCategoryUpdateRules)

        val progress = restoreProgress.incrementAndFetch()
        reporter.onRestoreProgress(
            context.stringResource(MR.strings.categories),
            progress,
            restoreAmount,
            isSync,
        )
    }

    private fun CoroutineScope.restoreEntries(
        entryRestore: EntryRestoreCoordinator,
        destinationProfileId: Long,
        backupEntries: Flow<BackupEntry>,
        backupCategories: List<BackupCategory>,
        categoriesJob: Job?,
    ) = launch {
        categoriesJob?.join()
        val finalization = entryRestore.restoreEntries(
            destinationProfileId = destinationProfileId,
            backupEntries = backupEntries,
            backupCategories = backupCategories,
            onBatchFailure = { error ->
                logcat(LogPriority.WARN, error) { "Batch restore failed, retrying entry by entry" }
            },
            onEntryFailure = { entry, error ->
                val sourceName = sourceMapping[entry.source] ?: entry.source.toString()
                errors.add(Date() to "${entry.title} [$sourceName]: ${error.message}")
            },
            onProgress = { lastEntry, restoredCount ->
                val progress = restoreProgress.addAndFetch(restoredCount)
                reporter.onRestoreProgress(lastEntry.title, progress, restoreAmount, isSync)
            },
        )
        finalization.issues.forEach { issue ->
            errors.add(Date() to issue.description)
        }
    }

    private fun CoroutineScope.restoreAppPreferences(
        profileId: Long,
        preferences: List<BackupPreference>,
        categories: List<BackupCategory>?,
        categoriesJob: Job?,
    ) = launch {
        ensureActive()
        categoriesJob?.join()
        preferenceRestorer.restoreAppForProfile(
            profileId = profileId,
            preferences = preferences,
            backupCategories = categories,
            includeGlobalRestore = true,
            scheduleJobs = true,
        )

        val progress = restoreProgress.incrementAndFetch()
        reporter.onRestoreProgress(
            context.stringResource(MR.strings.app_settings),
            progress,
            restoreAmount,
            isSync,
        )
    }

    private fun CoroutineScope.restoreSourcePreferences(
        profileId: Long,
        preferences: List<BackupSourcePreferences>,
    ) = launch {
        ensureActive()
        preferenceRestorer.restoreSource(profileId, preferences)

        val progress = restoreProgress.incrementAndFetch()
        reporter.onRestoreProgress(
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
                reporter.onRestoreProgress(
                    context.stringResource(MR.strings.extensionStores),
                    restoreProgress.load(),
                    restoreAmount,
                    isSync,
                )
            }
    }

    /** From the stores just restored, so only once those are in. */
    private fun installFileExtensions(backupFileExtensions: List<BackupFileExtension>) {
        fileExtensions.reinstall(backupFileExtensions)
    }

    /** A profile of the backup, read without its entries, which [entries] reads again from the file. */
    private class RestoredProfile(
        val backup: ProfileScopedBackup,
        val entryCount: Int,
        val entries: () -> Flow<BackupEntry>,
    )

    private fun writeErrorLog(): File {
        try {
            if (errors.isNotEmpty()) {
                val file = workingFiles.newRestoreErrorLog()
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
