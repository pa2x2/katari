package tachiyomi.domain.entry.interactor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import tachiyomi.domain.entry.model.DuplicateEntryCandidate
import tachiyomi.domain.entry.model.DuplicateMatchReason
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryMerge
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.MergedEntryRepository
import tachiyomi.domain.entry.service.DuplicateConfig
import tachiyomi.domain.entry.service.DuplicateEntryMetadata
import tachiyomi.domain.entry.service.DuplicateLibraryCandidate
import tachiyomi.domain.entry.service.DuplicateLibrarySupport
import tachiyomi.domain.entry.service.toDuplicateConfig
import tachiyomi.domain.library.service.DuplicatePreferences
import tachiyomi.domain.track.model.EntryTrack
import tachiyomi.domain.track.repository.TrackRepository

class GetDuplicateLibraryEntries(
    private val entryRepository: EntryRepository,
    private val mergedEntryRepository: MergedEntryRepository,
    private val entryChapterRepository: EntryChapterRepository,
    private val trackRepository: TrackRepository,
    private val duplicatePreferences: DuplicatePreferences,
) {

    suspend operator fun invoke(entry: Entry): List<DuplicateEntryCandidate> {
        val libraryEntries = entryRepository.getLibraryEntries()
        val merges = mergedEntryRepository.getAll()
        val tracks = trackRepository.getTracksAsFlow().first()
        val config = duplicatePreferences.toDuplicateConfig()
        return withContext(Dispatchers.Default) {
            detectDuplicates(entry, libraryEntries, merges, tracks, config)
        }
    }

    fun subscribe(
        entry: Flow<Entry>,
        scope: CoroutineScope,
    ): StateFlow<List<DuplicateEntryCandidate>> {
        val duplicateConfigFlow = combine(
            duplicatePreferences.extendedDuplicateDetectionEnabled.changes(),
            duplicatePreferences.minimumMatchScore.changes(),
            combine(
                duplicatePreferences.descriptionWeight.changes(),
                duplicatePreferences.authorWeight.changes(),
                duplicatePreferences.artistWeight.changes(),
                duplicatePreferences.coverWeight.changes(),
            ) { _, _, _, _ -> Unit },
            combine(
                duplicatePreferences.genreWeight.changes(),
                duplicatePreferences.statusWeight.changes(),
                duplicatePreferences.chapterCountWeight.changes(),
                duplicatePreferences.titleWeight.changes(),
            ) { _, _, _, _ -> Unit },
            duplicatePreferences.titleExclusionPatterns.changes(),
        ) { _, _, _, _, _ ->
            duplicatePreferences.toDuplicateConfig()
        }

        val snapshot = combine(
            entryRepository.getLibraryEntriesAsFlow(),
            mergedEntryRepository.subscribeAll(),
            trackRepository.getTracksAsFlow(),
            duplicateConfigFlow,
        ) { libraryEntries, merges, tracks, config ->
            DuplicateLibrarySnapshot(
                libraryEntries = libraryEntries,
                merges = merges,
                tracks = tracks,
                config = config,
            )
        }
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
                initialValue = DuplicateLibrarySnapshot(config = duplicatePreferences.toDuplicateConfig()),
            )

        return combine(entry, snapshot) { currentEntry, duplicateLibrarySnapshot ->
            detectDuplicates(
                entry = currentEntry,
                libraryEntries = duplicateLibrarySnapshot.libraryEntries,
                merges = duplicateLibrarySnapshot.merges,
                tracks = duplicateLibrarySnapshot.tracks,
                config = duplicateLibrarySnapshot.config,
            )
        }
            .distinctUntilChanged()
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
                initialValue = emptyList(),
            )
    }

    private suspend fun detectDuplicates(
        entry: Entry,
        libraryEntries: List<Entry>,
        merges: List<EntryMerge>,
        tracks: List<EntryTrack>,
        config: DuplicateConfig,
    ): List<DuplicateEntryCandidate> {
        val currentCount = entryChapterRepository.getChaptersByEntryIdAwait(entry.id).size.toLong()
        val current = entry.toDuplicateEntryMetadata(currentCount)

        val sameTypeLibraryEntries = libraryEntries.filter { it.type == entry.type }
        val excludedIds = buildExcludedIds(entry.id, merges)
        val libraryMemberIds = sameTypeLibraryEntries.map { it.id }.toSet()
        val trackerDuplicateIds = trackerDuplicateIds(entry.id, libraryMemberIds, tracks)

        val counts = sameTypeLibraryEntries.associate {
            it.id to
                entryChapterRepository.getChaptersByEntryIdAwait(it.id).size.toLong()
        }
        val candidates = buildCandidates(sameTypeLibraryEntries, merges, counts)

        return DuplicateLibrarySupport.detectDuplicates(
            currentEntry = current,
            libraryEntries = candidates,
            excludedIds = excludedIds,
            trackerDuplicateIds = trackerDuplicateIds,
            config = config,
        ).map { match ->
            DuplicateEntryCandidate(
                entry = match.item,
                count = match.count,
                cheapScore = match.cheapScore,
                scoreMax = match.scoreMax,
                score = match.score,
                reasons = match.reasons.map { DuplicateMatchReason.valueOf(it.name) },
                contentSignature = match.contentSignature,
            )
        }
    }

    private fun buildCandidates(
        libraryEntries: List<Entry>,
        merges: List<EntryMerge>,
        counts: Map<Long, Long>,
    ): List<DuplicateLibraryCandidate<Entry>> {
        val itemsById = libraryEntries.associateBy { it.id }
        val mergesByTargetId = merges.groupBy { it.targetId }
        val mergeByEntryId = merges.associateBy { it.entryId }

        val candidates = mutableListOf<DuplicateLibraryCandidate<Entry>>()
        val consumed = mutableSetOf<Long>()

        libraryEntries.forEach { entry ->
            if (!consumed.add(entry.id)) return@forEach

            val targetId = mergeByEntryId[entry.id]?.targetId
            val members = targetId
                ?.let { mergesByTargetId[it] }
                .orEmpty()
                .sortedBy { it.position }
                .mapNotNull { itemsById[it.entryId] }

            if (members.size > 1) {
                val target = members.firstOrNull { it.id == targetId } ?: members.first()
                val memberIds = members.map { it.id }
                val totalCount = memberIds.sumOf { counts[it] ?: 0L }
                candidates += DuplicateLibraryCandidate(
                    item = target,
                    sortTitle = target.displayTitle,
                    memberIds = memberIds,
                    memberEntries = members.map { it.toDuplicateEntryMetadata(totalCount) },
                    count = totalCount,
                    contentSignature = target.lastModifiedAt,
                )
                consumed += memberIds
            } else {
                val count = counts[entry.id] ?: 0L
                candidates += DuplicateLibraryCandidate(
                    item = entry,
                    sortTitle = entry.displayTitle,
                    memberIds = listOf(entry.id),
                    memberEntries = listOf(entry.toDuplicateEntryMetadata(count)),
                    count = count,
                    contentSignature = entry.lastModifiedAt,
                )
            }
        }

        return candidates
    }

    private fun buildExcludedIds(
        entryId: Long,
        merges: List<EntryMerge>,
    ): Set<Long> {
        val mergeTargetId = merges.firstOrNull { it.entryId == entryId }?.targetId
        if (mergeTargetId == null) {
            return setOf(entryId)
        }

        return merges.asSequence()
            .filter { it.targetId == mergeTargetId }
            .mapTo(linkedSetOf(mergeTargetId)) { it.entryId }
    }

    private fun trackerDuplicateIds(
        entryId: Long,
        libraryMemberIds: Set<Long>,
        tracks: List<EntryTrack>,
    ): Set<Long> {
        val currentTrackKeys = tracks.asSequence()
            .filter { it.entryId == entryId }
            .map { it.trackerId to it.remoteId }
            .toSet()

        if (currentTrackKeys.isEmpty()) return emptySet()

        return tracks.asSequence()
            .filter { it.entryId != entryId }
            .filter { it.entryId in libraryMemberIds }
            .filter { (it.trackerId to it.remoteId) in currentTrackKeys }
            .mapTo(linkedSetOf()) { it.entryId }
    }

    private fun Entry.toDuplicateEntryMetadata(count: Long?): DuplicateEntryMetadata {
        return DuplicateEntryMetadata(
            id = id,
            title = title,
            description = description,
            primaryCreator = author,
            secondaryCreator = artist,
            genres = genre,
            status = status.value.toLong(),
            count = count,
        )
    }

    private data class DuplicateLibrarySnapshot(
        val libraryEntries: List<Entry> = emptyList(),
        val merges: List<EntryMerge> = emptyList(),
        val tracks: List<EntryTrack> = emptyList(),
        val config: DuplicateConfig,
    )

    companion object {
        private const val SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L
    }
}
