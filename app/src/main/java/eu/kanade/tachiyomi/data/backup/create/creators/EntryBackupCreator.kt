package eu.kanade.tachiyomi.data.backup.create.creators

import dev.zacsweers.metro.Inject
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
import mihon.entry.interactions.persistence.backup.EntryFeatureStateEnvelope
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.GetHistoryByEntryIds
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.MergedMemberExpansionRepository
import tachiyomi.domain.history.model.activity.HistoryActivitySnapshot
import tachiyomi.domain.history.repository.HistoryActivityBackupRepository
import tachiyomi.domain.library.update.model.EntryUpdateMode
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository

@Inject
class EntryBackupCreator(
    private val handler: DatabaseHandler,
    private val profileProvider: ActiveProfileProvider,
    private val entryBackupFeature: EntryBackupFeature,
    private val entryChapterRepository: EntryChapterRepository,
    private val activityBackupRepository: HistoryActivityBackupRepository,
    private val updateRulesRepository: LibraryUpdateRulesRepository,
    private val mergedMemberExpansionRepository: MergedMemberExpansionRepository,
) {

    suspend operator fun invoke(
        entries: List<Entry>,
        options: BackupOptions,
        onEntry: (BackupEntry) -> Unit,
    ) {
        invoke(profileProvider.activeProfileId, entries, options, onEntry)
    }

    /**
     * Hands each of [entries] to [onEntry] as soon as it is backed up, so they needn't be held all at once. Entries are
     * read a chunk at a time, each kind of state with one query per chunk rather than one per entry.
     */
    suspend operator fun invoke(
        profileId: Long,
        entries: List<Entry>,
        options: BackupOptions,
        onEntry: (BackupEntry) -> Unit,
    ) {
        val statisticsEpoch = if (options.history) {
            activityBackupRepository.getStatisticsEpoch(profileId)
        } else {
            null
        }
        val updateModes = updateRulesRepository.getEntryModes(profileId)
        entries.chunked(CHUNK_SIZE).forEach { chunk ->
            backupEntries(profileId, chunk, options, statisticsEpoch, updateModes).forEach(onEntry)
        }
    }

    private suspend fun backupEntries(
        profileId: Long,
        entries: List<Entry>,
        options: BackupOptions,
        statisticsEpoch: Long?,
        updateModes: Map<Long, EntryUpdateMode>,
    ): List<BackupEntry> {
        val entryIds = entries.map(Entry::id)
        val expandedIds = mergedMemberExpansionRepository.getExpanded(entryIds)
        val featureStates = entryBackupFeature.snapshot(
            profileId = profileId,
            entries = entries,
            selection = EntryBackupSelection(
                includeContentState = options.chapters,
                includeTrackingState = options.tracking,
            ),
        )
        val chaptersByEntry = if (options.chapters || options.history) {
            entryChapterRepository.getChaptersByEntryIdsAwait(entryIds).groupBy(EntryChapter::entryId)
        } else {
            emptyMap()
        }
        val categoryOrdersByEntry = if (options.categories) {
            handler.awaitList { categoriesQueries.getCategoryOrdersByEntryIds(profileId, entryIds) }
                .groupBy({ it.entry_id }, { it.order })
        } else {
            emptyMap()
        }
        val historyByEntry = if (options.history) {
            handler.awaitList { historyQueries.getHistoryByEntryIds(entryIds) }.groupBy { it.entry_id }
        } else {
            emptyMap()
        }
        val activityByEntry = if (options.history) {
            activityBackupRepository.getActivityByEntryIds(entryIds)
        } else {
            emptyMap()
        }

        return entries.map { entry ->
            backupEntry(
                entry = entry,
                options = options,
                statisticsEpoch = statisticsEpoch,
                updateMode = updateModes[entry.id],
                expanded = entry.id in expandedIds,
                featureStates = featureStates[entry.id].orEmpty(),
                chapters = chaptersByEntry[entry.id].orEmpty(),
                categoryOrders = categoryOrdersByEntry[entry.id].orEmpty(),
                history = historyByEntry[entry.id].orEmpty(),
                activity = activityByEntry[entry.id],
            )
        }
    }

    private suspend fun backupEntry(
        entry: Entry,
        options: BackupOptions,
        statisticsEpoch: Long?,
        updateMode: EntryUpdateMode?,
        expanded: Boolean,
        featureStates: List<EntryFeatureStateEnvelope>,
        chapters: List<EntryChapter>,
        categoryOrders: List<Long>,
        history: List<GetHistoryByEntryIds>,
        activity: HistoryActivitySnapshot?,
    ): BackupEntry {
        val entryObject = entry.toBackupEntry()
        entryObject.updateMode = updateMode
            ?.takeIf { it != EntryUpdateMode.FOLLOW_RULES }
            ?.name
            ?.lowercase()
        entryObject.mergeMemberExpanded = expanded

        if (options.chapters && chapters.isNotEmpty()) {
            entryObject.chapters = chapters.map { it.toBackupChapter() }
        }

        if (options.categories && categoryOrders.isNotEmpty()) {
            entryObject.categories = categoryOrders
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

            val backupHistory = history.mapNotNull { row ->
                val url = chapterUrl(row.chapter_id) ?: return@mapNotNull null
                BackupHistory(url, row.last_read?.time ?: 0L, row.time_read)
            }
            if (backupHistory.isNotEmpty()) {
                entryObject.history = backupHistory
            }

            entryObject.activitySessions = activity?.sessions.orEmpty().map { session ->
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
            entryObject.activityCompletions = activity?.completions.orEmpty().map { completion ->
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

    private companion object {
        const val CHUNK_SIZE = 100
    }
}
