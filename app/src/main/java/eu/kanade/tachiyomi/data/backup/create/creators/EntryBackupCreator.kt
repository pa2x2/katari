package eu.kanade.tachiyomi.data.backup.create.creators

import eu.kanade.tachiyomi.data.backup.create.BackupOptions
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.BackupHistory
import eu.kanade.tachiyomi.data.backup.models.compatibility.applyLegacyFeatureStateProjection
import eu.kanade.tachiyomi.data.backup.models.toBackupChapter
import eu.kanade.tachiyomi.data.backup.models.toBackupEntry
import mihon.entry.interactions.EntryBackupFeature
import mihon.entry.interactions.EntryBackupSelection
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.history.model.History
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class EntryBackupCreator(
    private val handler: DatabaseHandler = Injekt.get(),
    private val profileProvider: ActiveProfileProvider = Injekt.get(),
    private val entryBackupFeature: EntryBackupFeature = Injekt.get(),
    private val entryChapterRepository: EntryChapterRepository = Injekt.get(),
) {

    suspend operator fun invoke(entries: List<Entry>, options: BackupOptions): List<BackupEntry> {
        return invoke(profileProvider.activeProfileId, entries, options)
    }

    suspend operator fun invoke(
        profileId: Long,
        entries: List<Entry>,
        options: BackupOptions,
    ): List<BackupEntry> {
        return entries.map { backupEntry(profileId, it, options) }
    }

    private suspend fun backupEntry(
        profileId: Long,
        entry: Entry,
        options: BackupOptions,
    ): BackupEntry {
        val entryObject = entry.toBackupEntry()
        val featureStates = entryBackupFeature.snapshot(
            profileId = profileId,
            entry = entry,
            selection = EntryBackupSelection(
                includeContentState = options.chapters,
                includeTrackingState = options.tracking,
            ),
        )

        if (options.chapters) {
            val chapters = entryChapterRepository.getChaptersByEntryIdAwait(entry.id, applyScanlatorFilter = false)
            if (chapters.isNotEmpty()) {
                entryObject.chapters = chapters.map { it.toBackupChapter() }
            }
        }

        if (options.categories) {
            val categoriesForEntry = handler.awaitList {
                categoriesQueries.getCategoriesByEntryId(
                    profileId,
                    entry.id,
                ) { id, name, order, flags ->
                    Category(
                        id = id,
                        name = name,
                        order = order,
                        flags = flags,
                    )
                }
            }
            if (categoriesForEntry.isNotEmpty()) {
                entryObject.categories = categoriesForEntry.map { it.order }
            }
        }

        if (options.history) {
            val historyByEntryId = handler.awaitList {
                historyQueries.getHistoryByEntryId(entry.id) { _, chapterId, lastRead, timeRead ->
                    History(
                        id = 0,
                        chapterId = chapterId,
                        readAt = lastRead,
                        readDuration = timeRead,
                    )
                }
            }
            if (historyByEntryId.isNotEmpty()) {
                val history = historyByEntryId.mapNotNull { history ->
                    val chapter = entryChapterRepository.getChapterById(history.chapterId) ?: return@mapNotNull null
                    BackupHistory(chapter.url, history.readAt?.time ?: 0L, history.readDuration)
                }
                if (history.isNotEmpty()) {
                    entryObject.history = history
                }
            }
        }

        entryObject.applyLegacyFeatureStateProjection(featureStates)

        return entryObject
    }
}
