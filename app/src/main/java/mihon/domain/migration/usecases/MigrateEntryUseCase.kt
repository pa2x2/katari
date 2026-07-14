package mihon.domain.migration.usecases

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.EntryTrackingSource
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.CancellationException
import mihon.domain.migration.models.MigrationFlag
import mihon.entry.interactions.EntryDownloadInteraction
import mihon.entry.interactions.EntryPlaybackPreferencesInteraction
import mihon.entry.interactions.EntryProgressInteraction
import mihon.entry.interactions.EntryProgressResourceMapping
import mihon.entry.viewer.settings.ViewerSettingOverrideRepository
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.entry.interactor.GetMergedEntry
import tachiyomi.domain.entry.interactor.SyncEntryWithSource
import tachiyomi.domain.entry.interactor.UpdateMergedEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.domain.util.MergeGroupMember
import tachiyomi.domain.util.MergeGroupReplacement
import tachiyomi.domain.util.replaceMergeGroupMember
import java.time.Instant

class MigrateEntryUseCase(
    private val sourcePreferences: SourcePreferences,
    private val trackerManager: TrackerManager,
    private val sourceManager: SourceManager,
    private val entryRepository: EntryRepository,
    private val entryChapterRepository: EntryChapterRepository,
    private val progressInteraction: EntryProgressInteraction,
    private val playbackPreferencesInteraction: EntryPlaybackPreferencesInteraction,
    private val downloadInteraction: EntryDownloadInteraction,
    private val categoryRepository: CategoryRepository,
    private val getTracks: GetTracks,
    private val insertTrack: InsertTrack,
    private val coverCache: CoverCache,
    private val getMergedEntry: GetMergedEntry,
    private val updateMergedEntry: UpdateMergedEntry,
    private val syncEntryWithSource: SyncEntryWithSource,
    private val viewerSettingOverrideRepository: ViewerSettingOverrideRepository,
) {

    private val enhancedServices by lazy { trackerManager.trackers.filterIsInstance<EnhancedTracker>() }

    suspend operator fun invoke(current: Entry, target: Entry, replace: Boolean) {
        if (current.type != target.type) return

        val targetSource = sourceManager.get(target.source)?.let {
            EntryTrackingSource.from(it, sourceManager.getDisplayInfo(target.source))
        }
        val currentSource = sourceManager.get(current.source)?.let {
            EntryTrackingSource.from(it, sourceManager.getDisplayInfo(current.source))
        }
        val flags = sourcePreferences.migrationFlags.get()

        try {
            syncEntryWithSource(target)

            val progressResourceMappings = if (MigrationFlag.CHAPTER in flags) {
                migrateChapters(current, target)
            } else {
                emptyList()
            }
            progressInteraction.copy(current, target, progressResourceMappings)
            playbackPreferencesInteraction.copy(current, target)
            viewerSettingOverrideRepository.copy(current.id, target.id)

            if (MigrationFlag.CATEGORY in flags) {
                val categoryIds = categoryRepository.getCategoriesByEntryId(current.id)
                    .map { it.id }
                entryRepository.setCategories(target.id, categoryIds)
            }

            getTracks.await(current.id).mapNotNull { track ->
                val updatedTrack = track.copy(entryId = target.id)

                val service = enhancedServices
                    .firstOrNull { it.isTrackFrom(updatedTrack, current, currentSource) }

                if (service != null && targetSource != null) {
                    service.migrateTrack(updatedTrack, target, targetSource)
                } else {
                    updatedTrack
                }
            }
                .takeIf { it.isNotEmpty() }
                ?.let { insertTrack.awaitAll(it) }

            if (MigrationFlag.REMOVE_DOWNLOAD in flags) {
                downloadInteraction.deleteEntryDownloads(current)
            }

            if (MigrationFlag.CUSTOM_COVER in flags && coverCache.getCustomCoverFile(current.id).exists()) {
                coverCache.setCustomCoverToCache(target.id, coverCache.getCustomCoverFile(current.id).inputStream())
            }

            val currentUpdate = if (replace) {
                current.copy(favorite = false, dateAdded = 0)
            } else {
                null
            }
            val targetUpdate = target.copy(
                favorite = true,
                chapterFlags = current.chapterFlags,
                viewerFlags = if (current.type == EntryType.MANGA) {
                    current.viewerFlags and LEGACY_MANGA_VIEWER_MASK.inv()
                } else {
                    current.viewerFlags
                },
                dateAdded = if (replace) current.dateAdded else Instant.now().toEpochMilli(),
                notes = if (MigrationFlag.NOTES in flags) current.notes else target.notes,
            )

            listOfNotNull(currentUpdate, targetUpdate).forEach { entryRepository.update(it) }

            if (replace) {
                updateMergeGroup(current.id, target.id)
            }
        } catch (e: Throwable) {
            if (e is CancellationException) {
                throw e
            }
        }
    }

    private suspend fun migrateChapters(current: Entry, target: Entry): List<EntryProgressResourceMapping> {
        val previousChapters = entryChapterRepository.getChaptersByEntryIdAwait(current.id)
        val targetChapters = entryChapterRepository.getChaptersByEntryIdAwait(target.id)

        val maxReadChapter = previousChapters
            .filter { it.read }
            .mapNotNull { it.chapterNumber.takeIf { number -> number >= 0.0 } }
            .maxOrNull()

        val chaptersToUpdate = mutableListOf<EntryChapter>()
        val progressResourceMappings = mutableListOf<EntryProgressResourceMapping>()

        targetChapters.forEach { targetChapter ->
            val previousChapter = findMatchingChapter(targetChapter, previousChapters)
            var read = targetChapter.read
            var bookmark = targetChapter.bookmark
            var dateFetch = targetChapter.dateFetch

            if (previousChapter != null) {
                read = previousChapter.read
                bookmark = previousChapter.bookmark
                dateFetch = previousChapter.dateFetch
                progressResourceMappings += EntryProgressResourceMapping(
                    sourceResourceKey = previousChapter.url,
                    targetResourceKey = targetChapter.url,
                    targetChapterId = targetChapter.id,
                )
            }

            if (maxReadChapter != null &&
                targetChapter.chapterNumber >= 0.0 &&
                targetChapter.chapterNumber <= maxReadChapter
            ) {
                read = true
            }

            val updatedChapter = targetChapter.copy(
                read = read,
                bookmark = bookmark,
                dateFetch = dateFetch,
            )
            if (updatedChapter != targetChapter) {
                chaptersToUpdate += updatedChapter
            }
        }

        if (chaptersToUpdate.isNotEmpty()) {
            entryChapterRepository.updateAll(chaptersToUpdate)
        }
        return progressResourceMappings
    }

    private fun findMatchingChapter(targetChapter: EntryChapter, previousChapters: List<EntryChapter>): EntryChapter? {
        return if (targetChapter.chapterNumber >= 0.0) {
            previousChapters.firstOrNull {
                it.chapterNumber >= 0.0 && it.chapterNumber == targetChapter.chapterNumber
            }
        } else {
            previousChapters.firstOrNull { it.name == targetChapter.name }
        }
    }

    private suspend fun updateMergeGroup(currentId: Long, targetId: Long) {
        val currentGroup = getMergedEntry.awaitGroupByEntryId(currentId)
        if (currentGroup.isEmpty()) return

        val targetGroup = getMergedEntry.awaitGroupByEntryId(targetId)
        val replacement = replaceMergeGroupMember(
            currentId = currentId,
            replacementId = targetId,
            currentGroup = currentGroup.map {
                MergeGroupMember(targetId = it.targetId, memberId = it.entryId, position = it.position)
            },
            replacementGroup = targetGroup.map {
                MergeGroupMember(targetId = it.targetId, memberId = it.entryId, position = it.position)
            },
        ) ?: return

        replacement.targetIdsToRemoveReplacementFrom.forEach { replacementTargetId ->
            updateMergedEntry.awaitRemoveMembers(replacementTargetId, listOf(targetId))
        }

        when (replacement) {
            is MergeGroupReplacement.Delete -> updateMergedEntry.awaitDeleteGroup(replacement.targetId)
            is MergeGroupReplacement.Upsert -> updateMergedEntry.awaitMerge(
                replacement.targetId,
                replacement.orderedMemberIds,
            )
        }
    }
}

private const val LEGACY_MANGA_VIEWER_MASK = 0x3FL
