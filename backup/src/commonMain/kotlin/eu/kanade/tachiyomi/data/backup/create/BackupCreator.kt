package eu.kanade.tachiyomi.data.backup.create

import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.backup.BackupFileValidator
import eu.kanade.tachiyomi.data.backup.create.creators.CategoriesBackupCreator
import eu.kanade.tachiyomi.data.backup.create.creators.EntryBackupCreator
import eu.kanade.tachiyomi.data.backup.create.creators.ExtensionStoresBackupCreator
import eu.kanade.tachiyomi.data.backup.create.creators.PreferenceBackupCreator
import eu.kanade.tachiyomi.data.backup.create.creators.SourcesBackupCreator
import eu.kanade.tachiyomi.data.backup.extensions.BackedUpFileExtensions
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupCategoryUpdateRules
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.BackupExtensionStore
import eu.kanade.tachiyomi.data.backup.models.BackupFileExtension
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import eu.kanade.tachiyomi.data.backup.sourcekeys.referencedSourceKeys
import eu.kanade.tachiyomi.data.backup.sourcekeys.sourceKeys
import eu.kanade.tachiyomi.data.backup.storage.BackupDocuments
import eu.kanade.tachiyomi.data.backup.storage.BackupWorkingFiles
import eu.kanade.tachiyomi.data.backup.stream.BackupWriter
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import mihon.core.platform.PlatformContext
import mihon.entry.interactions.persistence.backup.EntryBackupFeature
import mihon.feature.profiles.core.Profile
import mihon.feature.profiles.core.ProfileBackup
import mihon.feature.profiles.core.ProfileDatabase
import mihon.feature.profiles.core.ProfileScopedBackup
import mihon.feature.profiles.core.ProfileStore
import okio.buffer
import okio.gzip
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.backup.model.BackupOptions
import tachiyomi.domain.backup.service.BackupPreferences
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.storage.DocumentUri
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.stringResource
import kotlin.time.Clock

@AssistedInject
class BackupCreator(
    @Assisted private val isAutoBackup: Boolean,
    @Assisted private val onProgress: (BackupCreationProgress) -> Unit,
    private val context: PlatformContext,
    private val parser: ProtoBuf,
    private val documents: BackupDocuments,
    private val workingFiles: BackupWorkingFiles,
    private val backupPreferences: BackupPreferences,
    private val profileDatabase: ProfileDatabase,
    private val profileStore: ProfileStore,
    private val entryRepository: EntryRepository,
    private val entryBackupFeature: EntryBackupFeature,
    private val categoriesBackupCreator: CategoriesBackupCreator,
    private val entryBackupCreator: EntryBackupCreator,
    private val preferenceBackupCreator: PreferenceBackupCreator,
    private val extensionStoresBackupCreator: ExtensionStoresBackupCreator,
    private val fileExtensions: BackedUpFileExtensions,
    private val sourcesBackupCreator: SourcesBackupCreator,
    private val backupFileValidator: BackupFileValidator,
) {
    @AssistedFactory
    fun interface Factory {
        fun create(isAutoBackup: Boolean, onProgress: (BackupCreationProgress) -> Unit): BackupCreator
    }

    /** Backs up into the file at [uri], which is deleted again if the backup fails. */
    suspend fun backup(uri: DocumentUri, options: BackupOptions): DocumentUri {
        var isFile = false
        try {
            isFile = documents.isWritableFile(uri)
            if (!isFile) {
                throw IllegalStateException(context.stringResource(MR.strings.create_backup_file_error))
            }

            val allProfiles = profileDatabase.getProfiles(includeArchived = true)
            val activeProfile = allProfiles.firstOrNull { it.id == profileStore.currentProfileId }
            val profiles = allProfiles.filter { profile ->
                options.profileIds?.contains(profile.id) ?: true
            }
            val entriesByProfile = profiles.associate { it.id to entriesToBackUp(it.id, options) }
            // Restore reads only backupProfiles when the backup has any, and the schema seeds a default profile, so
            // repeating the active profile's entries at the top level would only double the work and the file size.
            val unscopedEntries = if (allProfiles.isEmpty()) {
                entriesToBackUp(activeProfile?.id, options)
            } else {
                emptyList()
            }

            val totalEntries = unscopedEntries.size + entriesByProfile.values.sumOf(List<Entry>::size)
            var backedUpEntries = 0
            val entrySourceKeys = hashSetOf<Long>()
            val onEntryBackedUp = { entry: BackupEntry ->
                entrySourceKeys += entry.sourceKeys()
                onProgress(BackupCreationProgress.Entries(++backedUpEntries, totalEntries))
            }
            if (totalEntries > 0) onProgress(BackupCreationProgress.Entries(0, totalEntries))

            documents.write(uri).gzip().buffer().use { sink ->
                val writer = BackupWriter(sink, parser, workingFiles.scratchDirectory)

                backupEntries(activeProfile?.id, unscopedEntries, options) { entry ->
                    writer.writeEntry(entry)
                    onEntryBackedUp(entry)
                }
                val backupProfiles = profiles.map { profile ->
                    backupProfile(profile, options).also { backup ->
                        writer.writeProfile(backup) { writeEntry ->
                            val profileId = profile.id
                            backupEntries(profileId, entriesByProfile.getValue(profileId), options) { entry ->
                                writeEntry(entry)
                                onEntryBackedUp(entry)
                            }
                        }
                    }
                }

                onProgress(BackupCreationProgress.Saving)
                val metadata = Backup(
                    backupCategories = backupCategories(options),
                    backupDefaultCategoryUpdateRules = backupDefaultCategoryUpdateRules(options),
                    backupPreferences = backupAppPreferences(options),
                    backupExtensionStores = backupExtensionStores(options),
                    backupFileExtensions = backupFileExtensions(options),
                    backupSourcePreferences = backupSourcePreferences(options),
                    backupProfiles = backupProfiles,
                    activeProfileUuid = activeProfile?.uuid,
                    kitsuScoresNative = true,
                )
                metadata.backupSources = sourcesBackupCreator(entrySourceKeys + metadata.referencedSourceKeys())
                writer.writeMetadata(metadata)

                if (writer.isEmpty) {
                    throw IllegalStateException(context.stringResource(MR.strings.empty_backup_error))
                }
            }

            // Make sure it's a valid backup file
            backupFileValidator.validate(uri)

            if (isAutoBackup) {
                backupPreferences.lastAutoBackupTimestamp.set(Clock.System.now().toEpochMilliseconds())
            }

            return uri
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            if (isFile) documents.delete(uri)
            throw e
        }
    }

    private suspend fun backupCategories(options: BackupOptions): List<BackupCategory> {
        if (!options.categories) return emptyList()

        return categoriesBackupCreator()
    }

    private suspend fun backupDefaultCategoryUpdateRules(options: BackupOptions): BackupCategoryUpdateRules? {
        if (!options.categories) return null
        return categoriesBackupCreator.defaultCategoryUpdateRules()
    }

    private suspend fun entriesToBackUp(profileId: Long?, options: BackupOptions): List<Entry> {
        if (!options.libraryEntries) return emptyList()

        val entries = if (profileId != null) {
            entryRepository.getFavoritesByProfile(profileId) +
                if (options.readEntries) entryRepository.getReadEntriesNotInLibraryByProfile(profileId) else emptyList()
        } else {
            entryRepository.getFavorites() +
                if (options.readEntries) entryRepository.getReadEntriesNotInLibrary() else emptyList()
        }
        return entries.groupBy(Entry::profileId).flatMap { (entryProfileId, profileEntries) ->
            entryBackupFeature.withCompanionEntries(entryProfileId, profileEntries)
        }
    }

    private suspend fun backupEntries(
        profileId: Long?,
        entries: List<Entry>,
        options: BackupOptions,
        onEntry: (BackupEntry) -> Unit,
    ) {
        if (profileId != null) {
            entryBackupCreator(profileId, entries, options, onEntry)
        } else {
            entryBackupCreator(entries, options, onEntry)
        }
    }

    private fun backupAppPreferences(options: BackupOptions): List<BackupPreference> {
        if (!options.appSettings) return emptyList()

        return preferenceBackupCreator.createApp(includePrivatePreferences = options.privateSettings)
    }

    private suspend fun backupExtensionStores(options: BackupOptions): List<BackupExtensionStore> {
        if (!options.extensionStores) return emptyList()

        return extensionStoresBackupCreator()
    }

    // Installed from their stores on restore, so they're kept with them
    private fun backupFileExtensions(options: BackupOptions): List<BackupFileExtension> {
        if (!options.extensionStores) return emptyList()

        return fileExtensions.installed()
    }

    private suspend fun backupSourcePreferences(options: BackupOptions): List<BackupSourcePreferences> {
        if (!options.sourceSettings) return emptyList()

        return preferenceBackupCreator.createSource(includePrivatePreferences = options.privateSettings)
    }

    /** [profile] without its entries, which are written separately. */
    private suspend fun backupProfile(profile: Profile, options: BackupOptions): ProfileScopedBackup {
        val profileId = profile.id

        val categories = if (options.categories) {
            categoriesBackupCreator(profileId)
        } else {
            emptyList()
        }
        val defaultCategoryUpdateRules = if (options.categories) {
            categoriesBackupCreator.defaultCategoryUpdateRules(profileId)
        } else {
            null
        }

        val appPreferences = if (options.appSettings) {
            preferenceBackupCreator.createApp(
                profileId = profileId,
                includePrivatePreferences = options.privateSettings,
            )
        } else {
            emptyList()
        }

        val sourcePreferences = if (options.sourceSettings) {
            preferenceBackupCreator.createSource(
                profileId = profileId,
                includePrivatePreferences = options.privateSettings,
            )
        } else {
            emptyList()
        }

        return ProfileScopedBackup(
            profile = ProfileBackup(
                uuid = profile.uuid,
                name = profile.name,
                colorSeed = profile.colorSeed,
                position = profile.position,
                // A profile's lock is one of its security settings; the database column is only read to move it there.
                requiresAuth = SecurityPreferences(profileStore.profileStore(profileId)).useAuthenticator.get(),
                isArchived = profile.isArchived,
            ),
            categories = categories,
            defaultCategoryUpdateRules = defaultCategoryUpdateRules,
            preferences = appPreferences,
            sourcePreferences = sourcePreferences,
        )
    }
}
