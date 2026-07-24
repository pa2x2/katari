package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupChapter
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.BackupHistory
import eu.kanade.tachiyomi.data.backup.models.compatibility.featureStatesWithLegacyFallback
import eu.kanade.tachiyomi.data.backup.models.compatibility.normalizeLegacyViewerFlags
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryBackupFeature
import mihon.entry.interactions.EntryBackupRestoreFinalization
import mihon.entry.interactions.EntryBackupRestoreSession
import mihon.entry.interactions.EntryBackupRestoreSessionId
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryIdentity
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.service.FetchInterval
import tachiyomi.domain.history.interactor.UpsertHistory
import tachiyomi.domain.history.model.History
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.history.repository.HistoryRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.ZonedDateTime
import java.util.Date
import java.util.UUID
import kotlin.math.max

class EntryRestorer(
    private val handler: DatabaseHandler = Injekt.get(),
    private val profileProvider: ActiveProfileProvider = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val entryRepository: EntryRepository = Injekt.get(),
    private val entryChapterRepository: EntryChapterRepository = Injekt.get(),
    private val entryBackupFeature: EntryBackupFeature = Injekt.get(),
    private val upsertHistory: UpsertHistory = Injekt.get(),
    private val historyRepository: HistoryRepository = Injekt.get(),
    fetchInterval: FetchInterval = Injekt.get(),
) {

    private var now = ZonedDateTime.now()
    private var currentFetchWindow = fetchInterval.getWindow(now)
    private val restoreSession = EntryBackupRestoreSession(EntryBackupRestoreSessionId(UUID.randomUUID().toString()))
    private val restoredTypesByProfile = linkedMapOf<Long, MutableSet<EntryType>>()

    init {
        now = ZonedDateTime.now()
        currentFetchWindow = fetchInterval.getWindow(now)
    }

    suspend fun sortByNew(backupEntries: List<BackupEntry>): List<BackupEntry> {
        val existingEntries = handler.awaitList {
            entriesQueries.getAllEntriesSourceAndUrl(profileProvider.activeProfileId)
        }.mapTo(hashSetOf()) {
            EntryIdentity(
                profileId = profileProvider.activeProfileId,
                source = it.source,
                url = it.url,
                type = EntryType.valueOf(it.type.uppercase()),
            )
        }

        return backupEntries
            .sortedWith(
                compareBy<BackupEntry> {
                    EntryIdentity(
                        profileId = profileProvider.activeProfileId,
                        source = it.source,
                        url = it.url,
                        type = it.type,
                    ) in existingEntries
                }
                    .then(compareByDescending { it.lastModifiedAt }),
            )
    }

    suspend fun restore(
        backupEntry: BackupEntry,
        backupCategories: List<BackupCategory>,
    ) {
        handler.await(inTransaction = true) {
            val dbEntry = findExistingEntry(backupEntry)
            val entry = backupEntry.toEntry()
            val restoredEntry = if (dbEntry == null) {
                entry.copy(id = entryRepository.insert(entry))
            } else {
                restoreExistingEntry(entry, dbEntry)
            }

            val normalizedEntry = backupEntry.normalizeLegacyViewerFlags(restoredEntry)
            restoreEntryDetails(
                entry = normalizedEntry,
                backupEntry = backupEntry,
                backupCategories = backupCategories,
            )
            restoredTypesByProfile.getOrPut(profileProvider.activeProfileId, ::linkedSetOf) += normalizedEntry.type
        }
    }

    suspend fun finalizeFeatureRestore(destinationProfileId: Long): EntryBackupRestoreFinalization {
        return entryBackupFeature.finalizeRestore(
            restoreSession,
            destinationProfileId,
            restoredTypesByProfile.remove(destinationProfileId).orEmpty(),
        )
    }

    private suspend fun findExistingEntry(backupEntry: BackupEntry): Entry? {
        return entryRepository.getEntryByUrlAndSourceId(
            backupEntry.url,
            backupEntry.source,
            backupEntry.type,
            profileProvider.activeProfileId,
        )
    }

    private suspend fun restoreExistingEntry(entry: Entry, dbEntry: Entry): Entry {
        val merged = if (entry.version > dbEntry.version) {
            dbEntry.copyFrom(entry).copy(id = dbEntry.id)
        } else {
            entry.copyFrom(dbEntry).copy(id = dbEntry.id)
        }
        entryRepository.update(merged)
        return merged
    }

    private fun Entry.copyFrom(newer: Entry): Entry {
        return this.copy(
            favorite = this.favorite || newer.favorite,
            author = newer.author,
            artist = newer.artist,
            description = newer.description,
            genre = newer.genre,
            thumbnailUrl = newer.thumbnailUrl,
            status = newer.status,
            initialized = this.initialized || newer.initialized,
            version = newer.version,
            memo = newer.memo,
        )
    }

    private suspend fun restoreEntryDetails(
        entry: Entry,
        backupEntry: BackupEntry,
        backupCategories: List<BackupCategory>,
    ) {
        restoreCategories(entry, backupEntry.categories, backupCategories)
        restoreChapters(entry, backupEntry.chapters)
        restoreHistory(entry, backupEntry.history)
        entryBackupFeature.restore(
            session = restoreSession,
            profileId = profileProvider.activeProfileId,
            entry = entry,
            states = backupEntry.featureStatesWithLegacyFallback(entry),
        )
        val withInterval = FetchInterval(
            entryChapterRepository,
        ).update(entry, now, currentFetchWindow)
        entryRepository.update(withInterval)
    }

    private suspend fun restoreCategories(
        entry: Entry,
        categories: List<Long>,
        backupCategories: List<BackupCategory>,
    ) {
        val dbCategories = getCategories.await()
        val dbCategoriesByName = dbCategories.associateBy { it.name }
        val backupCategoriesByOrder = backupCategories.associateBy { it.order }

        val entryCategoriesToUpdate = categories.mapNotNull { backupCategoryOrder ->
            backupCategoriesByOrder[backupCategoryOrder]?.let { backupCategory ->
                dbCategoriesByName[backupCategory.name]?.let { dbCategory ->
                    Pair(entry.id, dbCategory.id)
                }
            }
        }

        if (entryCategoriesToUpdate.isNotEmpty()) {
            handler.await(inTransaction = true) {
                entries_categoriesQueries.deleteByEntryId(
                    profileProvider.activeProfileId,
                    entry.id,
                )
                entryCategoriesToUpdate.forEach { (entryId, categoryId) ->
                    entries_categoriesQueries.insert(
                        profileProvider.activeProfileId,
                        entryId,
                        categoryId,
                    )
                }
            }
        }
    }

    private suspend fun restoreChapters(entry: Entry, backupChapters: List<BackupChapter>) {
        val dbChaptersByUrl = entryChapterRepository.getChaptersByEntryIdAwait(entry.id)
            .associateBy { it.url }

        val chaptersToUpsert = backupChapters.mapNotNull { backupChapter ->
            val chapter = backupChapter.toEntryChapter(entry.id)
            val dbChapter = dbChaptersByUrl[chapter.url]
                ?: return@mapNotNull chapter

            if (chapter.forComparison() == dbChapter.forComparison()) {
                return@mapNotNull null
            }

            var updatedChapter = chapter
                .copyFrom(dbChapter)
                .copy(
                    id = dbChapter.id,
                    bookmark = chapter.bookmark || dbChapter.bookmark,
                )
            if (dbChapter.read && !updatedChapter.read) {
                updatedChapter = updatedChapter.copy(
                    read = true,
                )
            }
            updatedChapter
        }

        entryChapterRepository.insertOrUpdate(chaptersToUpsert)
    }

    private fun tachiyomi.domain.entry.model.EntryChapter.forComparison() =
        this.copy(id = 0L, entryId = 0L, dateFetch = 0L, dateUpload = 0L, lastModifiedAt = 0L, version = 0L)

    private suspend fun restoreHistory(entry: Entry, backupHistory: List<BackupHistory>) {
        if (backupHistory.isEmpty()) return
        val existingHistoryByChapterId = historyRepository.getHistoryByEntryId(entry.id)
            .associateBy { it.chapterId }
        backupHistory.forEach { history ->
            val chapter = entryChapterRepository.getChapterByUrlAndEntryId(history.url, entry.id)
                ?: return@forEach
            upsertHistory.await(history.mergeWith(chapter.id, existingHistoryByChapterId[chapter.id]))
        }
    }
}

internal fun BackupHistory.mergeWith(chapterId: Long, existingHistory: History?): HistoryUpdate {
    val existingReadAt = existingHistory?.readAt?.time ?: 0L
    val existingDuration = existingHistory?.readDuration ?: 0L
    return HistoryUpdate(
        chapterId = chapterId,
        readAt = Date(max(lastRead, existingReadAt)),
        sessionReadDuration = max(readDuration, existingDuration) - existingDuration,
    )
}
