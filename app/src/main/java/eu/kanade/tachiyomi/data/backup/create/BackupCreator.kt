package eu.kanade.tachiyomi.data.backup.create

import android.content.Context
import android.net.Uri
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.backup.BackupFileValidator
import eu.kanade.tachiyomi.data.backup.create.creators.CategoriesBackupCreator
import eu.kanade.tachiyomi.data.backup.create.creators.EntryBackupCreator
import eu.kanade.tachiyomi.data.backup.create.creators.ExtensionStoresBackupCreator
import eu.kanade.tachiyomi.data.backup.create.creators.PreferenceBackupCreator
import eu.kanade.tachiyomi.data.backup.create.creators.SourcesBackupCreator
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupCategoryUpdateRules
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.BackupExtensionStore
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import mihon.feature.profiles.core.ProfileBackup
import mihon.feature.profiles.core.ProfileBundle
import mihon.feature.profiles.core.ProfileManager
import mihon.feature.profiles.core.ProfileScopedBackup
import okio.buffer
import okio.gzip
import okio.sink
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.backup.service.BackupPreferences
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.i18n.*
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Clock

class BackupCreator(
    private val context: Context,
    private val isAutoBackup: Boolean,
    private val onProgress: (BackupCreationProgress) -> Unit = {},

    private val parser: ProtoBuf = Injekt.get(),
    private val backupPreferences: BackupPreferences = Injekt.get(),
    private val profileManager: ProfileManager = Injekt.get(),
    private val entryRepository: EntryRepository = Injekt.get(),

    private val categoriesBackupCreator: CategoriesBackupCreator = CategoriesBackupCreator(),
    private val entryBackupCreator: EntryBackupCreator = EntryBackupCreator(),
    private val preferenceBackupCreator: PreferenceBackupCreator = PreferenceBackupCreator(),
    private val extensionStoresBackupCreator: ExtensionStoresBackupCreator = ExtensionStoresBackupCreator(),
    private val sourcesBackupCreator: SourcesBackupCreator = SourcesBackupCreator(),
) {

    suspend fun backup(uri: Uri, options: BackupOptions): String {
        var file: UniFile? = null
        try {
            file = if (isAutoBackup) {
                // Get dir of file and create
                val dir = UniFile.fromUri(context, uri)

                // Delete older backups
                dir?.listFiles { _, filename -> FILENAME_REGEX.matches(filename) }
                    .orEmpty()
                    .sortedByDescending { it.name }
                    .drop(MAX_AUTO_BACKUPS - 1)
                    .forEach { it.delete() }

                // Create new file to place backup
                dir?.createFile(getFilename())
            } else {
                UniFile.fromUri(context, uri)
            }

            if (file == null || !file.isFile) {
                throw IllegalStateException(context.stringResource(MR.strings.create_backup_file_error))
            }

            val activeProfile = profileManager.activeProfile.value
            val profileBundles = profileManager.getProfileBundles(includeArchived = true)
            val entriesByProfile = profileBundles.associate { it.profile.id to entriesToBackUp(it.profile.id, options) }
            // Restore reads only backupProfiles when the backup has any, and the schema seeds a default profile, so
            // repeating the active profile's entries at the top level would only double the work and the file size.
            val unscopedEntries = if (profileBundles.isEmpty()) {
                entriesToBackUp(activeProfile?.id, options)
            } else {
                emptyList()
            }

            val totalEntries = unscopedEntries.size + entriesByProfile.values.sumOf(List<Entry>::size)
            var backedUpEntries = 0
            val onEntryBackedUp = { onProgress(BackupCreationProgress.Entries(++backedUpEntries, totalEntries)) }
            if (totalEntries > 0) onProgress(BackupCreationProgress.Entries(0, totalEntries))

            val backupEntries = backupEntries(activeProfile?.id, unscopedEntries, options, onEntryBackedUp)
            val backupProfiles = backupProfiles(profileBundles, entriesByProfile, options, onEntryBackedUp)
            val backupSources = backupSources(
                entries = backupEntries + backupProfiles.flatMap(ProfileScopedBackup::entries),
            )

            val backup = Backup(
                backupCategories = backupCategories(options),
                backupDefaultCategoryUpdateRules = backupDefaultCategoryUpdateRules(options),
                backupSources = backupSources,
                backupPreferences = backupAppPreferences(options),
                backupExtensionStores = backupExtensionStores(options),
                backupSourcePreferences = backupSourcePreferences(options),
                backupProfiles = backupProfiles,
                activeProfileUuid = activeProfile?.uuid,
                backupEntries = backupEntries,
            )

            onProgress(BackupCreationProgress.Saving)
            val byteArray = parser.encodeToByteArray(Backup.serializer(), backup)
            if (byteArray.isEmpty()) {
                throw IllegalStateException(context.stringResource(MR.strings.empty_backup_error))
            }

            file.openOutputStream()
                .also {
                    // Force overwrite old file
                    (it as? FileOutputStream)?.channel?.truncate(0)
                }
                .sink().gzip().buffer().use {
                    it.write(byteArray)
                }
            val fileUri = file.uri

            // Make sure it's a valid backup file
            BackupFileValidator(context).validate(fileUri)

            if (isAutoBackup) {
                backupPreferences.lastAutoBackupTimestamp.set(Clock.System.now().toEpochMilliseconds())
            }

            return fileUri.toString()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            file?.delete()
            throw e
        }
    }

    private suspend fun backupCategories(options: BackupOptions): List<BackupCategory> {
        if (!options.categories) return emptyList()

        val activeProfileId = profileManager.activeProfile.value?.id
        return if (activeProfileId != null) {
            categoriesBackupCreator(activeProfileId)
        } else {
            categoriesBackupCreator()
        }
    }

    private suspend fun backupDefaultCategoryUpdateRules(options: BackupOptions): BackupCategoryUpdateRules? {
        if (!options.categories) return null
        val activeProfileId = profileManager.activeProfile.value?.id
        return if (activeProfileId != null) {
            categoriesBackupCreator.defaultCategoryUpdateRules(activeProfileId)
        } else {
            categoriesBackupCreator.defaultCategoryUpdateRules()
        }
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
        return entries.distinctBy { it.id }
    }

    private suspend fun backupEntries(
        profileId: Long?,
        entries: List<Entry>,
        options: BackupOptions,
        onEntryBackedUp: () -> Unit,
    ): List<BackupEntry> {
        return if (profileId != null) {
            entryBackupCreator(profileId, entries, options, onEntryBackedUp)
        } else {
            entryBackupCreator(entries, options, onEntryBackedUp)
        }
    }

    private fun backupSources(entries: List<BackupEntry>): List<BackupSource> {
        return sourcesBackupCreator(entries)
    }

    private fun backupAppPreferences(options: BackupOptions): List<BackupPreference> {
        if (!options.appSettings) return emptyList()

        return preferenceBackupCreator.createApp(includePrivatePreferences = options.privateSettings)
    }

    private suspend fun backupExtensionStores(options: BackupOptions): List<BackupExtensionStore> {
        if (!options.extensionStores) return emptyList()

        return extensionStoresBackupCreator()
    }

    private fun backupSourcePreferences(options: BackupOptions): List<BackupSourcePreferences> {
        if (!options.sourceSettings) return emptyList()

        return preferenceBackupCreator.createSource(includePrivatePreferences = options.privateSettings)
    }

    private suspend fun backupProfiles(
        bundles: List<ProfileBundle>,
        entriesByProfile: Map<Long, List<Entry>>,
        options: BackupOptions,
        onEntryBackedUp: () -> Unit,
    ): List<ProfileScopedBackup> {
        return bundles.map { bundle ->
            val profileId = bundle.profile.id
            val entries = backupEntries(profileId, entriesByProfile.getValue(profileId), options, onEntryBackedUp)

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

            ProfileScopedBackup(
                profile = ProfileBackup(
                    uuid = bundle.profile.uuid,
                    name = bundle.profile.name,
                    colorSeed = bundle.profile.colorSeed,
                    position = bundle.profile.position,
                    requiresAuth = bundle.profile.requiresAuth,
                    isArchived = bundle.profile.isArchived,
                ),
                categories = categories,
                defaultCategoryUpdateRules = defaultCategoryUpdateRules,
                entries = entries,
                preferences = appPreferences,
                sourcePreferences = sourcePreferences,
            )
        }
    }

    companion object {
        private const val MAX_AUTO_BACKUPS: Int = 4
        private const val FILENAME_PREFIX = "katari"
        private val FILENAME_REGEX = """${FILENAME_PREFIX}_\d{4}-\d{2}-\d{2}_\d{2}-\d{2}\.tachibk""".toRegex()

        fun getFilename(): String {
            val date = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.ENGLISH).format(Date())
            return "${FILENAME_PREFIX}_$date.tachibk"
        }
    }
}
