package eu.kanade.tachiyomi.data.backup.create.creators

import eu.kanade.tachiyomi.data.backup.create.BackupOptions
import eu.kanade.tachiyomi.data.backup.models.BackupDownloadPreferences
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.BackupHistory
import eu.kanade.tachiyomi.data.backup.models.BackupPlaybackPreferences
import eu.kanade.tachiyomi.data.backup.models.backupTrackMapper
import eu.kanade.tachiyomi.data.backup.models.toBackupChapter
import eu.kanade.tachiyomi.data.backup.models.toBackupEntry
import eu.kanade.tachiyomi.data.backup.models.toBackupEntryProgressState
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryPlaybackPreferencesInteraction
import mihon.entry.interactions.EntryPlaybackQualityMode
import mihon.entry.interactions.EntryProgressInteraction
import mihon.entry.interactions.EntryProgressSnapshot
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryMerge
import tachiyomi.domain.entry.repository.DownloadPreferencesRepository
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.history.model.History
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class EntryBackupCreator(
    private val handler: DatabaseHandler = Injekt.get(),
    private val profileProvider: ActiveProfileProvider = Injekt.get(),
    private val entryRepository: EntryRepository = Injekt.get(),
    private val entryChapterRepository: EntryChapterRepository = Injekt.get(),
    private val downloadPreferencesRepository: DownloadPreferencesRepository = Injekt.get(),
    private val progressInteraction: EntryProgressInteraction = Injekt.get(),
    private val playbackPreferencesInteraction: EntryPlaybackPreferencesInteraction = Injekt.get(),
) {

    suspend operator fun invoke(entries: List<Entry>, options: BackupOptions): List<BackupEntry> {
        return invoke(profileProvider.activeProfileId, entries, options)
    }

    suspend operator fun invoke(
        profileId: Long,
        entries: List<Entry>,
        options: BackupOptions,
    ): List<BackupEntry> {
        val allEntriesById = entryRepository.getAllEntriesByProfile(profileId).associateBy { it.id }
        return entries.map { backupEntry(profileId, it, options, allEntriesById) }
    }

    private suspend fun backupEntry(
        profileId: Long,
        entry: Entry,
        options: BackupOptions,
        allEntriesById: Map<Long, Entry>,
    ): BackupEntry {
        val entryObject = entry.toBackupEntry()
        val playbackPreferencesSnapshot = if (entry.type == EntryType.ANIME) {
            playbackPreferencesInteraction.snapshot(entry)
        } else {
            null
        }
        val progressSnapshot = if (options.chapters) {
            progressInteraction.snapshot(entry)
        } else {
            EntryProgressSnapshot()
        }

        if (entry.type == EntryType.MANGA) {
            entryObject.excludedScanlators = handler.awaitList {
                excluded_scanlatorsQueries.getExcludedScanlatorsByEntryId(profileId, entry.id)
            }
        }

        if (options.chapters) {
            val chapters = entryChapterRepository.getChaptersByEntryIdAwait(entry.id, applyScanlatorFilter = false)
            if (chapters.isNotEmpty()) {
                entryObject.chapters = chapters.map { it.toBackupChapter() }
            }

            entryObject.progressStates = progressSnapshot.states.map { it.toBackupEntryProgressState() }
        }

        if (entry.type == EntryType.ANIME) {
            if (options.chapters) {
                val downloadPreferences = downloadPreferencesRepository.getByEntryId(entry.id)
                if (downloadPreferences != null) {
                    entryObject.downloadPreferences = BackupDownloadPreferences(
                        dubKey = downloadPreferences.dubKey,
                        streamKey = downloadPreferences.streamKey,
                        subtitleKey = downloadPreferences.subtitleKey,
                        qualityMode = downloadPreferences.qualityMode.name.lowercase(),
                        updatedAt = downloadPreferences.updatedAt,
                    )
                }
            }

            val preferences = playbackPreferencesSnapshot
            if (preferences != null) {
                entryObject.playbackPreferences = BackupPlaybackPreferences(
                    dubKey = preferences.dubKey,
                    streamKey = preferences.streamKey,
                    sourceQualityKey = preferences.sourceQualityKey,
                    subtitleKey = preferences.subtitleKey,
                    playerQualityMode = preferences.playerQualityMode.toBackupValue(),
                    playerQualityHeight = preferences.playerQualityHeight,
                    subtitleOffsetX = preferences.subtitleOffsetX,
                    subtitleOffsetY = preferences.subtitleOffsetY,
                    subtitleTextSize = preferences.subtitleTextSize,
                    subtitleTextColor = preferences.subtitleTextColor,
                    subtitleBackgroundColor = preferences.subtitleBackgroundColor,
                    subtitleBackgroundOpacity = preferences.subtitleBackgroundOpacity,
                    updatedAt = preferences.updatedAt,
                )
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

        if (options.tracking) {
            val tracks = handler.awaitList {
                entry_syncQueries.getTracksByEntryId(profileId, entry.id, backupTrackMapper)
            }
            if (tracks.isNotEmpty()) {
                entryObject.tracking = tracks
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

        val mergeGroup = getMergeGroupForBackup(profileId, entry.id)
        if (mergeGroup.isNotEmpty()) {
            val targetId = mergeGroup.first().targetId
            val targetEntry = allEntriesById[targetId]
            val position = mergeGroup.firstOrNull { it.entryId == entry.id }?.position?.toInt()
            if (targetEntry != null && position != null) {
                entryObject.mergeTargetSource = targetEntry.source
                entryObject.mergeTargetUrl = targetEntry.url
                entryObject.mergeTargetType = targetEntry.type
                entryObject.mergePosition = position
            }
        }

        return entryObject
    }

    private suspend fun getMergeGroupForBackup(profileId: Long, entryId: Long): List<EntryMerge> {
        return handler.awaitList {
            merged_entriesQueries.getEntriesByEntryId(profileId, entryId) { targetEntryId, memberEntryId, position ->
                EntryMerge(targetId = targetEntryId, entryId = memberEntryId, position = position)
            }
        }
    }
}

private fun EntryPlaybackQualityMode.toBackupValue(): String {
    return when (this) {
        EntryPlaybackQualityMode.AUTO -> "auto"
        EntryPlaybackQualityMode.SPECIFIC_HEIGHT -> "specific_height"
    }
}
