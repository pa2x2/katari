package eu.kanade.tachiyomi.data.backup.restore.restorers

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupChapter
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.BackupHistory
import eu.kanade.tachiyomi.data.backup.models.compatibility.featureStatesWithLegacyFallback
import eu.kanade.tachiyomi.data.backup.models.compatibility.normalizeLegacyViewerFlags
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mihon.entry.interactions.persistence.backup.EntryBackupFeature
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreFinalization
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreSession
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreSessionId
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryIdentity
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.MergedMemberExpansionRepository
import tachiyomi.domain.entry.service.nextupdate.FetchInterval
import tachiyomi.domain.history.interactor.UpsertHistory
import tachiyomi.domain.history.model.History
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.history.model.activity.HistoryActivitySegmentSnapshot
import tachiyomi.domain.history.model.activity.HistoryActivitySessionSnapshot
import tachiyomi.domain.history.model.activity.HistoryActivitySnapshot
import tachiyomi.domain.history.model.activity.HistoryCompletionCause
import tachiyomi.domain.history.model.activity.HistoryCompletionSnapshot
import tachiyomi.domain.history.repository.HistoryActivityBackupRepository
import tachiyomi.domain.history.repository.HistoryRepository
import tachiyomi.domain.library.update.model.EntryUpdateMode
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository
import java.util.Date
import java.util.UUID
import kotlin.math.max
import kotlin.time.Clock

@Inject
class EntryRestorer(
    private val handler: DatabaseHandler,
    private val getCategories: GetCategories,
    private val entryRepository: EntryRepository,
    private val entryChapterRepository: EntryChapterRepository,
    private val entryBackupFeature: EntryBackupFeature,
    private val upsertHistory: UpsertHistory,
    private val historyRepository: HistoryRepository,
    private val activityBackupRepository: HistoryActivityBackupRepository,
    private val fetchInterval: FetchInterval,
    private val updateRulesRepository: LibraryUpdateRulesRepository,
    private val mergedMemberExpansionRepository: MergedMemberExpansionRepository,
) {

    private val timeZone = TimeZone.currentSystemDefault()
    private var now = Clock.System.now().toLocalDateTime(timeZone)
    private var currentFetchWindow = fetchInterval.getWindow(now.date, timeZone)
    private val restoreSession = EntryBackupRestoreSession(EntryBackupRestoreSessionId(UUID.randomUUID().toString()))
    private val restoredTypesByProfile = linkedMapOf<Long, MutableSet<EntryType>>()

    init {
        now = Clock.System.now().toLocalDateTime(timeZone)
        currentFetchWindow = fetchInterval.getWindow(now.date, timeZone)
    }

    suspend fun sortByNew(profileId: Long, backupEntries: List<BackupEntry>): List<BackupEntry> {
        val existingEntries = handler.awaitList {
            entriesQueries.getAllEntriesSourceAndUrl(profileId)
        }.mapTo(hashSetOf()) {
            EntryIdentity(
                profileId = profileId,
                source = it.source,
                url = it.url,
                type = EntryType.valueOf(it.type.uppercase()),
            )
        }

        return backupEntries
            .sortedWith(
                compareBy<BackupEntry> {
                    EntryIdentity(
                        profileId = profileId,
                        source = it.source,
                        url = it.url,
                        type = it.type,
                    ) in existingEntries
                }
                    .then(compareByDescending { it.lastModifiedAt }),
            )
    }

    suspend fun restore(
        profileId: Long,
        backupEntry: BackupEntry,
        backupCategories: List<BackupCategory>,
    ) {
        handler.await(inTransaction = true) {
            val dbEntry = findExistingEntry(profileId, backupEntry)
            val entry = backupEntry.toEntry().copy(profileId = profileId)
            val restoredEntry = if (dbEntry == null) {
                entry.copy(id = entryRepository.insert(entry, profileId))
            } else {
                restoreExistingEntry(profileId, entry, dbEntry)
            }

            val normalizedEntry = backupEntry.normalizeLegacyViewerFlags(restoredEntry)
            restoreEntryDetails(
                profileId = profileId,
                entry = normalizedEntry,
                backupEntry = backupEntry,
                backupCategories = backupCategories,
            )
            restoredTypesByProfile.getOrPut(profileId, ::linkedSetOf) += normalizedEntry.type
        }
    }

    suspend fun finalizeFeatureRestore(destinationProfileId: Long): EntryBackupRestoreFinalization {
        return entryBackupFeature.finalizeRestore(
            restoreSession,
            destinationProfileId,
            restoredTypesByProfile.remove(destinationProfileId).orEmpty(),
        )
    }

    private suspend fun findExistingEntry(profileId: Long, backupEntry: BackupEntry): Entry? {
        return entryRepository.getEntryByUrlAndSourceId(
            backupEntry.url,
            backupEntry.source,
            backupEntry.type,
            profileId,
        )
    }

    private suspend fun restoreExistingEntry(profileId: Long, entry: Entry, dbEntry: Entry): Entry {
        val merged = if (entry.version > dbEntry.version) {
            dbEntry.copyFrom(entry).copy(id = dbEntry.id)
        } else {
            entry.copyFrom(dbEntry).copy(id = dbEntry.id)
        }
        entryRepository.update(merged, profileId)
        return merged
    }

    private fun Entry.copyFrom(newer: Entry): Entry {
        return this.copy(
            favorite = this.favorite || newer.favorite,
            libraryPinned = newer.libraryPinned,
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
        profileId: Long,
        entry: Entry,
        backupEntry: BackupEntry,
        backupCategories: List<BackupCategory>,
    ) {
        restoreCategories(profileId, entry, backupEntry.categories, backupCategories)
        restoreChapters(entry, backupEntry.chapters)
        restoreHistory(entry, backupEntry.history)
        restoreActivity(profileId, entry, backupEntry)
        backupEntry.updateMode?.let { mode ->
            updateRulesRepository.setEntryMode(listOf(entry.id), EntryUpdateMode.valueOf(mode.uppercase()))
        }
        if (backupEntry.mergeMemberExpanded) {
            mergedMemberExpansionRepository.setExpanded(entry.id, expanded = true)
        }
        entryBackupFeature.restore(
            session = restoreSession,
            profileId = profileId,
            entry = entry,
            states = backupEntry.featureStatesWithLegacyFallback(entry),
        )
        val withInterval = fetchInterval.update(entry, now, timeZone, currentFetchWindow).let { updated ->
            // A next update still ahead was worked out from the chapters the backup came from.
            backupEntry.nextUpdate.takeIf { it >= currentFetchWindow.first }
                ?.let { updated.copy(nextUpdate = it) }
                ?: updated
        }
        entryRepository.update(withInterval, profileId)
    }

    private suspend fun restoreCategories(
        profileId: Long,
        entry: Entry,
        categories: List<Long>,
        backupCategories: List<BackupCategory>,
    ) {
        val dbCategories = getCategories.awaitForProfile(profileId)
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
                    profileId,
                    entry.id,
                )
                entryCategoriesToUpdate.forEach { (entryId, categoryId) ->
                    entries_categoriesQueries.insert(
                        profileId,
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

        // A duplicated chapter is backed up once per copy; writing each copy would let the last one decide its state.
        val restoredChapters = backupChapters
            .map { it.toEntryChapter(entry.id) }
            .groupBy { it.url }
            .map { (_, copies) ->
                copies.reduce { kept, other ->
                    kept.copy(read = kept.read || other.read, bookmark = kept.bookmark || other.bookmark)
                }
            }

        val chaptersToUpsert = restoredChapters.mapNotNull { chapter ->
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
        // A duplicated chapter is backed up once per copy, so merging each copy against the stored row separately
        // would add its reading time more than once.
        backupHistory.groupBy { it.url }.forEach { (url, copies) ->
            val chapter = entryChapterRepository.getChapterByUrlAndEntryId(url, entry.id)
                ?: return@forEach
            val history = BackupHistory(
                url = url,
                lastRead = copies.maxOf { it.lastRead },
                readDuration = copies.sumOf { it.readDuration },
            )
            upsertHistory.await(history.mergeWith(chapter.id, existingHistoryByChapterId[chapter.id]))
        }
    }

    private suspend fun restoreActivity(profileId: Long, entry: Entry, backupEntry: BackupEntry) {
        if (
            backupEntry.activitySessions.isEmpty() &&
            backupEntry.activityCompletions.isEmpty() &&
            backupEntry.statisticsEpoch == null
        ) {
            return
        }

        val chapterUrls = buildSet {
            backupEntry.activitySessions.forEach { session ->
                session.segments.mapNotNullTo(this) { it.chapterUrl }
            }
            backupEntry.activityCompletions.mapNotNullTo(this) { it.chapterUrl }
        }
        val chapterIdsByUrl = buildMap {
            chapterUrls.forEach { url ->
                put(url, entryChapterRepository.getChapterByUrlAndEntryId(url, entry.id)?.id)
            }
        }
        val snapshot = HistoryActivitySnapshot(
            sessions = backupEntry.activitySessions.map { session ->
                HistoryActivitySessionSnapshot(
                    sessionId = session.sessionId,
                    startedAtEpochMillis = session.startedAt,
                    endedAtEpochMillis = session.endedAt,
                    durationMillis = session.duration,
                    lastSequence = session.lastSequence,
                    segments = session.segments.map { segment ->
                        HistoryActivitySegmentSnapshot(
                            chapterId = segment.chapterUrl?.let(chapterIdsByUrl::get),
                            localDate = segment.localDate,
                            timeZoneId = segment.timeZoneId,
                            startedAtEpochMillis = segment.startedAt,
                            endedAtEpochMillis = segment.endedAt,
                            durationMillis = segment.duration,
                        )
                    },
                )
            },
            completions = backupEntry.activityCompletions.mapNotNull { completion ->
                val cause = HistoryCompletionCause.entries.firstOrNull {
                    it.storageValue == completion.cause
                } ?: return@mapNotNull null
                HistoryCompletionSnapshot(
                    eventId = completion.eventId,
                    chapterId = completion.chapterUrl?.let(chapterIdsByUrl::get),
                    sessionId = completion.sessionId,
                    occurredAtEpochMillis = completion.occurredAt,
                    localDate = completion.localDate,
                    timeZoneId = completion.timeZoneId,
                    cause = cause,
                )
            },
        )
        activityBackupRepository.restoreActivity(entry.id, snapshot)
        backupEntry.statisticsEpoch?.let { epoch ->
            activityBackupRepository.restoreStatisticsEpoch(profileId, epoch)
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
