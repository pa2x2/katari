package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupChapter
import eu.kanade.tachiyomi.data.backup.models.BackupDownloadPreferences
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.BackupEntryProgressState
import eu.kanade.tachiyomi.data.backup.models.BackupHistory
import eu.kanade.tachiyomi.data.backup.models.BackupPlaybackPreferences
import eu.kanade.tachiyomi.data.backup.models.BackupPlaybackState
import eu.kanade.tachiyomi.data.backup.models.BackupTracking
import eu.kanade.tachiyomi.data.backup.models.toEntryProgressStateSnapshot
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryPlaybackPreferencesInteraction
import mihon.entry.interactions.EntryPlaybackPreferencesSnapshot
import mihon.entry.interactions.EntryPlaybackQualityMode
import mihon.entry.interactions.EntryProgressInteraction
import mihon.entry.interactions.EntryProgressSnapshot
import mihon.entry.interactions.EntryProgressStateSnapshot
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.entry.interactor.UpdateMergedEntry
import tachiyomi.domain.entry.model.DownloadPreferences
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryIdentity
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.model.VideoDownloadQualityMode
import tachiyomi.domain.entry.model.identity
import tachiyomi.domain.entry.repository.DownloadPreferencesRepository
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.service.FetchInterval
import tachiyomi.domain.history.interactor.UpsertHistory
import tachiyomi.domain.history.model.History
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.history.repository.HistoryRepository
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.domain.track.model.EntryTrack
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.ZonedDateTime
import java.util.Date
import kotlin.math.max

class EntryRestorer(
    private val handler: DatabaseHandler = Injekt.get(),
    private val profileProvider: ActiveProfileProvider = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val entryRepository: EntryRepository = Injekt.get(),
    private val entryChapterRepository: EntryChapterRepository = Injekt.get(),
    private val downloadPreferencesRepository: DownloadPreferencesRepository = Injekt.get(),
    private val progressInteraction: EntryProgressInteraction = Injekt.get(),
    private val playbackPreferencesInteraction: EntryPlaybackPreferencesInteraction = Injekt.get(),
    private val upsertHistory: UpsertHistory = Injekt.get(),
    private val historyRepository: HistoryRepository = Injekt.get(),
    private val getTracks: GetTracks = Injekt.get(),
    private val insertTrack: InsertTrack = Injekt.get(),
    private val updateMergedEntry: UpdateMergedEntry = Injekt.get(),
    fetchInterval: FetchInterval = Injekt.get(),
) {

    private var now = ZonedDateTime.now()
    private var currentFetchWindow = fetchInterval.getWindow(now)
    private val pendingMerges = linkedMapOf<EntryIdentity, PendingMergeGroup>()

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

            restoreEntryDetails(
                entry = restoredEntry,
                backupEntry = backupEntry,
                backupCategories = backupCategories,
            )

            enqueueMerge(restoredEntry, backupEntry)
        }
    }

    suspend fun restorePendingMerges() {
        pendingMerges.values.forEach { merge ->
            val targetEntry = entryRepository.getEntryByUrlAndSourceId(
                merge.targetIdentity.url,
                merge.targetIdentity.source,
                merge.targetIdentity.type,
                profileProvider.activeProfileId,
            ) ?: return@forEach
            val orderedIds = merge.members
                .let { members ->
                    if (members.any { it.identity == merge.targetIdentity }) {
                        members
                    } else {
                        members +
                            PendingMergeMember(
                                identity = merge.targetIdentity,
                                position = Int.MIN_VALUE,
                            )
                    }
                }
                .filter { it.identity.type == merge.targetIdentity.type }
                .sortedBy { it.position }
                .mapNotNull { member ->
                    entryRepository.getEntryByUrlAndSourceId(
                        member.identity.url,
                        member.identity.source,
                        member.identity.type,
                        profileProvider.activeProfileId,
                    )?.id
                }
                .distinct()

            if (targetEntry.id in orderedIds && orderedIds.size > 1) {
                updateMergedEntry.awaitMerge(targetEntry.id, orderedIds)
            }
        }
        pendingMerges.clear()
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
        if (entry.type == EntryType.MANGA) {
            restoreTracking(entry, backupEntry.tracking)
            restoreExcludedScanlators(entry, backupEntry.excludedScanlators)
        }
        restorePlaybackPreferences(entry, backupEntry.playbackPreferences)
        restoreProgress(entry, backupEntry.playbackStates, backupEntry.progressStates)
        if (entry.type == EntryType.ANIME) {
            restoreDownloadPreferences(entry, backupEntry.downloadPreferences)
        }
        val withInterval = FetchInterval(
            entryChapterRepository,
        ).update(entry, now, currentFetchWindow)
        entryRepository.update(withInterval)
    }

    private suspend fun restoreProgress(
        entry: Entry,
        legacyPlaybackStates: List<BackupPlaybackState>,
        states: List<BackupEntryProgressState>,
    ) {
        val genericStates = states.map { it.toEntryProgressStateSnapshot() }
        val genericIdentities = genericStates.mapTo(hashSetOf()) { it.contentKey to it.resourceKey }
        val legacyStates = legacyPlaybackStates
            .mapNotNull { it.toProgressSnapshot() }
            .filterNot { (it.contentKey to it.resourceKey) in genericIdentities }
        progressInteraction.restore(
            entry,
            EntryProgressSnapshot(
                states = legacyStates + genericStates,
            ),
        )
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
                    lastPageRead = dbChapter.lastPageRead,
                )
            } else if (updatedChapter.lastPageRead == 0L && dbChapter.lastPageRead != 0L) {
                updatedChapter = updatedChapter.copy(
                    lastPageRead = dbChapter.lastPageRead,
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

    private suspend fun restoreTracking(entry: Entry, backupTracks: List<BackupTracking>) {
        val dbTrackByTrackerId = getTracks.await(entry.id).associateBy { it.trackerId }

        val (existingTracks, newTracks) = backupTracks
            .mapNotNull {
                val track = it.getTrackImpl()
                val dbTrack = dbTrackByTrackerId[track.trackerId]
                    ?: return@mapNotNull track.copy(
                        id = 0,
                        entryId = entry.id,
                    )

                if (track.forComparison() == dbTrack.forComparison()) {
                    return@mapNotNull null
                }

                dbTrack.copy(
                    remoteId = track.remoteId,
                    libraryId = track.libraryId,
                    progress = max(dbTrack.progress, track.progress),
                )
            }
            .partition { it.id > 0 }

        if (newTracks.isNotEmpty()) {
            insertTrack.awaitAll(newTracks)
        }
        if (existingTracks.isNotEmpty()) {
            handler.await(inTransaction = true) {
                existingTracks.forEach { track ->
                    entry_syncQueries.update(
                        entryId = track.entryId,
                        syncId = track.trackerId,
                        mediaId = track.remoteId,
                        libraryId = track.libraryId,
                        title = track.title,
                        lastChapterRead = track.progress,
                        totalChapter = track.total,
                        status = track.status,
                        score = track.score,
                        trackingUrl = track.remoteUrl,
                        startDate = track.startDate,
                        finishDate = track.finishDate,
                        private = track.private,
                        id = track.id,
                        profileId = profileProvider.activeProfileId,
                    )
                }
            }
        }
    }

    private fun EntryTrack.forComparison() = this.copy(id = 0L, entryId = 0L)

    private suspend fun restoreExcludedScanlators(entry: Entry, excludedScanlators: List<String>) {
        if (excludedScanlators.isEmpty()) return
        val existingExcludedScanlators = handler.awaitList {
            excluded_scanlatorsQueries.getExcludedScanlatorsByEntryId(profileProvider.activeProfileId, entry.id)
        }
        val toInsert = excludedScanlators.filter { it !in existingExcludedScanlators }
        if (toInsert.isNotEmpty()) {
            handler.await {
                toInsert.forEach {
                    excluded_scanlatorsQueries.insert(profileProvider.activeProfileId, entry.id, it)
                }
            }
        }
    }

    private suspend fun restorePlaybackPreferences(
        entry: Entry,
        backupPreferences: BackupPlaybackPreferences?,
    ) {
        val preferences = backupPreferences?.toPlaybackSnapshot() ?: return
        playbackPreferencesInteraction.restore(entry, preferences)
    }

    private fun BackupPlaybackState.toProgressSnapshot(): EntryProgressStateSnapshot? {
        if (url.isBlank() || (!completed && positionMs <= 0L)) return null
        val safePosition = positionMs.coerceAtLeast(0L)
        val safeDuration = durationMs.takeIf { it > 0L }
        val timestamp = lastWatchedAt.coerceAtLeast(0L)
        return EntryProgressStateSnapshot(
            resourceKey = url,
            sourceChildKey = url,
            locator = EntryProgressLocator(
                kind = "time",
                position = safePosition,
                extent = safeDuration,
                progression = safeDuration?.let {
                    (safePosition.toDouble() / it.toDouble()).coerceIn(0.0, 1.0)
                },
            ),
            completed = completed,
            locatorUpdatedAt = timestamp,
            completionUpdatedAt = timestamp,
        )
    }

    private suspend fun restoreDownloadPreferences(entry: Entry, backupPreferences: BackupDownloadPreferences?) {
        if (backupPreferences == null) return
        downloadPreferencesRepository.upsert(
            DownloadPreferences(
                entryId = entry.id,
                dubKey = backupPreferences.dubKey,
                streamKey = backupPreferences.streamKey,
                subtitleKey = backupPreferences.subtitleKey,
                qualityMode = backupPreferences.qualityMode.toDownloadQualityMode(),
                updatedAt = backupPreferences.updatedAt,
            ),
        )
    }

    private fun String.toDownloadQualityMode(): VideoDownloadQualityMode {
        return when (this) {
            "best" -> VideoDownloadQualityMode.BEST
            "data_saving" -> VideoDownloadQualityMode.DATA_SAVING
            else -> VideoDownloadQualityMode.BALANCED
        }
    }

    private fun BackupPlaybackPreferences.toPlaybackSnapshot(): EntryPlaybackPreferencesSnapshot {
        return EntryPlaybackPreferencesSnapshot(
            dubKey = dubKey,
            streamKey = streamKey,
            sourceQualityKey = sourceQualityKey,
            subtitleKey = subtitleKey,
            playerQualityMode = playerQualityMode.toPlaybackQualityMode(),
            playerQualityHeight = playerQualityHeight,
            subtitleOffsetX = subtitleOffsetX,
            subtitleOffsetY = subtitleOffsetY,
            subtitleTextSize = subtitleTextSize,
            subtitleTextColor = subtitleTextColor,
            subtitleBackgroundColor = subtitleBackgroundColor,
            subtitleBackgroundOpacity = subtitleBackgroundOpacity,
            updatedAt = updatedAt,
        )
    }

    private fun String.toPlaybackQualityMode(): EntryPlaybackQualityMode {
        return when (this) {
            "specific_height" -> EntryPlaybackQualityMode.SPECIFIC_HEIGHT
            else -> EntryPlaybackQualityMode.AUTO
        }
    }

    private fun enqueueMerge(entry: Entry, backupEntry: BackupEntry) {
        val targetSource = backupEntry.mergeTargetSource ?: return
        val targetUrl = backupEntry.mergeTargetUrl ?: return
        val position = backupEntry.mergePosition ?: return
        val targetType = backupEntry.mergeTargetType ?: backupEntry.type
        val targetIdentity = EntryIdentity(entry.profileId, targetSource, targetUrl, targetType)
        val memberIdentity = entry.identity()

        val group = pendingMerges.getOrPut(targetIdentity) {
            PendingMergeGroup(
                targetIdentity = targetIdentity,
                members = mutableListOf(),
            )
        }
        group.members.removeAll { it.identity == memberIdentity }
        group.members.add(PendingMergeMember(identity = memberIdentity, position = position))
        if (targetIdentity == memberIdentity) {
            group.members.removeAll { it.identity == targetIdentity }
            group.members.add(PendingMergeMember(identity = targetIdentity, position = position))
        }
    }

    private data class PendingMergeGroup(
        val targetIdentity: EntryIdentity,
        val members: MutableList<PendingMergeMember>,
    )

    private data class PendingMergeMember(
        val identity: EntryIdentity,
        val position: Int,
    )
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
