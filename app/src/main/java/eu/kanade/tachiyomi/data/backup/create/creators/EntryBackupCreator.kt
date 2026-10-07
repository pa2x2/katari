package eu.kanade.tachiyomi.data.backup.create.creators

import eu.kanade.tachiyomi.data.backup.create.BackupOptions
import eu.kanade.tachiyomi.data.backup.models.BackupActivityCompletion
import eu.kanade.tachiyomi.data.backup.models.BackupActivitySegment
import eu.kanade.tachiyomi.data.backup.models.BackupActivitySession
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.BackupHistory
import eu.kanade.tachiyomi.data.backup.models.compatibility.applyLegacyFeatureStateProjection
import eu.kanade.tachiyomi.data.backup.models.toBackupChapter
import eu.kanade.tachiyomi.data.backup.models.toBackupEntry
import mihon.entry.interactions.persistence.backup.EntryBackupFeature
import mihon.entry.interactions.persistence.backup.EntryBackupSelection
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.history.model.History
import tachiyomi.domain.history.repository.HistoryActivityBackupRepository
import tachiyomi.domain.library.update.model.EntryUpdateMode
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class EntryBackupCreator(
    private val handler: DatabaseHandler = Injekt.get(),
    private val profileProvider: ActiveProfileProvider = Injekt.get(),
    private val entryBackupFeature: EntryBackupFeature = Injekt.get(),
    private val entryChapterRepository: EntryChapterRepository = Injekt.get(),
    private val activityBackupRepository: HistoryActivityBackupRepository = Injekt.get(),
    private val updateRulesRepository: LibraryUpdateRulesRepository = Injekt.get(),
) {

    suspend operator fun invoke(
        entries: List<Entry>,
        options: BackupOptions,
        onEntryBackedUp: () -> Unit,
    ): List<BackupEntry> {
        return invoke(profileProvider.activeProfileId, entries, options, onEntryBackedUp)
    }

    suspend operator fun invoke(
        profileId: Long,
        entries: List<Entry>,
        options: BackupOptions,
        onEntryBackedUp: () -> Unit,
    ): List<BackupEntry> {
        val statisticsEpoch = if (options.history) {
            activityBackupRepository.getStatisticsEpoch(profileId)
        } else {
            null
        }
        return entries.map { entry ->
            backupEntry(profileId, entry, options, statisticsEpoch).also { onEntryBackedUp() }
        }
    }

    private suspend fun backupEntry(
        profileId: Long,
        entry: Entry,
        options: BackupOptions,
        statisticsEpoch: Long?,
    ): BackupEntry {
        val entryObject = entry.toBackupEntry()
        entryObject.updateMode = updateRulesRepository.getEntryMode(entry.id)
            .takeIf { it != EntryUpdateMode.FOLLOW_RULES }
            ?.name
            ?.lowercase()
        val featureStates = entryBackupFeature.snapshot(
            profileId = profileId,
            entry = entry,
            selection = EntryBackupSelection(
                includeContentState = options.chapters,
                includeTrackingState = options.tracking,
            ),
        )

        val chapters = if (options.chapters || options.history) {
            entryChapterRepository.getChaptersByEntryIdAwait(entry.id, applyScanlatorFilter = false)
        } else {
            emptyList()
        }
        if (options.chapters && chapters.isNotEmpty()) {
            entryObject.chapters = chapters.map { it.toBackupChapter() }
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
            // History and activity point at this entry's chapters, so looking each one up separately would cost a
            // query per history row; only ids missing from the list, such as deleted chapters, still need one.
            val chapterUrls = chapters.associateTo(mutableMapOf<Long, String?>()) { it.id to it.url }
            suspend fun chapterUrl(chapterId: Long?): String? {
                if (chapterId == null) return null
                if (chapterId !in chapterUrls) {
                    chapterUrls[chapterId] = entryChapterRepository.getChapterById(chapterId)?.url
                }
                return chapterUrls[chapterId]
            }

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
                    val url = chapterUrl(history.chapterId) ?: return@mapNotNull null
                    BackupHistory(url, history.readAt?.time ?: 0L, history.readDuration)
                }
                if (history.isNotEmpty()) {
                    entryObject.history = history
                }
            }

            val activity = activityBackupRepository.getActivityByEntryId(entry.id)
            entryObject.activitySessions = activity.sessions.map { session ->
                BackupActivitySession(
                    sessionId = session.sessionId,
                    startedAt = session.startedAtEpochMillis,
                    endedAt = session.endedAtEpochMillis,
                    duration = session.durationMillis,
                    lastSequence = session.lastSequence,
                    segments = session.segments.map { segment ->
                        BackupActivitySegment(
                            chapterUrl = chapterUrl(segment.chapterId),
                            localDate = segment.localDate,
                            timeZoneId = segment.timeZoneId,
                            startedAt = segment.startedAtEpochMillis,
                            endedAt = segment.endedAtEpochMillis,
                            duration = segment.durationMillis,
                        )
                    },
                )
            }
            entryObject.activityCompletions = activity.completions.map { completion ->
                BackupActivityCompletion(
                    eventId = completion.eventId,
                    chapterUrl = chapterUrl(completion.chapterId),
                    sessionId = completion.sessionId,
                    occurredAt = completion.occurredAtEpochMillis,
                    localDate = completion.localDate,
                    timeZoneId = completion.timeZoneId,
                    cause = completion.cause.storageValue,
                )
            }
            entryObject.statisticsEpoch = statisticsEpoch
        }

        entryObject.applyLegacyFeatureStateProjection(featureStates)

        return entryObject
    }
}
