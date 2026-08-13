package mihon.entry.interactions.host

import app.cash.sqldelight.async.coroutines.awaitAsList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mihon.entry.interactions.merge.host.EntryMergeMembershipSnapshot
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.entry.EntryMapper
import tachiyomi.domain.entry.model.DuplicateEntryCandidate
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.service.DuplicateConfig
import tachiyomi.domain.entry.service.DuplicateEntryMetadata
import tachiyomi.domain.entry.service.DuplicateLibraryCandidate
import tachiyomi.domain.entry.service.DuplicateLibrarySupport
import tachiyomi.domain.entry.service.toDuplicateConfig
import tachiyomi.domain.library.service.DuplicatePreferences

internal class AppEntryMergeDuplicateCandidateHost(
    private val handler: DatabaseHandler,
    private val preferences: DuplicatePreferences,
) {
    suspend fun candidates(
        profileId: Long,
        entry: Entry,
        memberships: List<EntryMergeMembershipSnapshot>,
    ): List<DuplicateEntryCandidate> {
        return detect(
            entry = entry,
            libraryEntries = libraryEntries(profileId),
            memberships = memberships,
            tracks = tracks(profileId),
            config = preferences.toDuplicateConfig(),
        )
    }

    fun observeCandidates(
        profileId: Long,
        entry: Flow<Entry>,
        memberships: Flow<List<EntryMergeMembershipSnapshot>>,
    ): Flow<List<DuplicateEntryCandidate>> {
        val entriesWithCounts = observeEntriesWithCounts(profileId, entry)
        val tracks = trackIdentityKeys(profileId)
        return combine(
            entriesWithCounts,
            memberships,
            tracks,
            duplicateConfig(),
        ) { entries, currentMemberships, currentTracks, config ->
            detect(
                entry = entries.current,
                sameTypeLibraryEntries = entries.library,
                memberships = currentMemberships,
                tracks = currentTracks,
                config = config,
                counts = entries.counts,
            )
        }
    }

    private fun observeEntriesWithCounts(
        profileId: Long,
        entry: Flow<Entry>,
    ): Flow<DuplicateCandidateEntries> {
        val libraryEntries = handler.subscribeToList {
            entriesQueries.getFavorites(profileId, EntryMapper::mapEntry)
        }
        return observeDuplicateCandidateEntries(entry, libraryEntries) { entryIds ->
            val invalidations = handler.subscribeToList {
                chaptersQueries.getCountsByEntryIds(listOf(entryIds.first())) { entryId, count -> entryId to count }
            }.map { Unit }
            observeDuplicateCandidateCounts(invalidations) { loadCounts(entryIds) }
        }
    }

    private fun duplicateConfig(): Flow<DuplicateConfig> {
        return combine(
            preferences.extendedDuplicateDetectionEnabled.changes(),
            preferences.minimumMatchScore.changes(),
            combine(
                preferences.descriptionWeight.changes(),
                preferences.authorWeight.changes(),
                preferences.artistWeight.changes(),
                preferences.coverWeight.changes(),
            ) { _, _, _, _ -> Unit },
            combine(
                preferences.genreWeight.changes(),
                preferences.statusWeight.changes(),
                preferences.chapterCountWeight.changes(),
                preferences.titleWeight.changes(),
            ) { _, _, _, _ -> Unit },
            preferences.titleExclusionPatterns.changes(),
        ) { _, _, _, _, _ -> preferences.toDuplicateConfig() }
    }

    private suspend fun libraryEntries(profileId: Long): List<Entry> {
        return handler.awaitList { entriesQueries.getFavorites(profileId, EntryMapper::mapEntry) }
    }

    private fun trackIdentityKeys(profileId: Long): Flow<List<DuplicateCandidateTrackKey>> {
        return handler.subscribeToList {
            entry_syncQueries.getTrackIdentityKeys(profileId, ::DuplicateCandidateTrackKey)
        }.distinctUntilChanged()
    }

    private suspend fun tracks(profileId: Long): List<DuplicateCandidateTrackKey> {
        return handler.awaitList {
            entry_syncQueries.getTrackIdentityKeys(profileId, ::DuplicateCandidateTrackKey)
        }
    }

    private suspend fun detect(
        entry: Entry,
        libraryEntries: List<Entry>,
        memberships: List<EntryMergeMembershipSnapshot>,
        tracks: List<DuplicateCandidateTrackKey>,
        config: DuplicateConfig,
    ): List<DuplicateEntryCandidate> {
        require(libraryEntries.all { it.profileId == entry.profileId }) {
            "Merge candidate data cannot cross profiles"
        }
        val sameTypeLibraryEntries = libraryEntries.filter { it.type == entry.type }
        val entryIds = (sameTypeLibraryEntries.map(Entry::id) + entry.id).distinct()
        val counts = if (entryIds.isEmpty()) {
            emptyMap()
        } else {
            loadCounts(entryIds)
        }
        return detect(entry, sameTypeLibraryEntries, memberships, tracks, config, counts)
    }

    private suspend fun loadCounts(entryIds: List<Long>): Map<Long, Long> {
        return handler.await(inTransaction = true) {
            loadDuplicateCandidateCounts(entryIds) { entryIdChunk ->
                chaptersQueries.getCountsByEntryIds(entryIdChunk) { entryId, count -> entryId to count }
                    .awaitAsList()
                    .toMap()
            }
        }
    }

    private suspend fun detect(
        entry: Entry,
        sameTypeLibraryEntries: List<Entry>,
        memberships: List<EntryMergeMembershipSnapshot>,
        tracks: List<DuplicateCandidateTrackKey>,
        config: DuplicateConfig,
        counts: Map<Long, Long>,
    ): List<DuplicateEntryCandidate> = withContext(Dispatchers.Default) {
        val membershipByEntry = memberships
            .flatMap { membership -> membership.orderedEntryIds.map { it to membership } }
            .toMap()
        val current = entry.toMetadata(counts[entry.id])
        val excludedIds = membershipByEntry[entry.id]?.orderedEntryIds?.toSet() ?: setOf(entry.id)
        val libraryMemberIds = sameTypeLibraryEntries.mapTo(mutableSetOf(), Entry::id)
        val trackerDuplicates = duplicateCandidateTrackEntryIds(entry.id, libraryMemberIds, tracks)
        val candidates = buildCandidates(sameTypeLibraryEntries, memberships, counts)
        DuplicateLibrarySupport.detectDuplicates(
            currentEntry = current,
            libraryEntries = candidates,
            excludedIds = excludedIds,
            trackerDuplicateIds = trackerDuplicates,
            config = config,
        ).map { match ->
            DuplicateEntryCandidate(
                entry = match.item,
                count = match.count,
                cheapScore = match.cheapScore,
                scoreMax = match.scoreMax,
                score = match.score,
                reasons = match.reasons,
                contentSignature = match.contentSignature,
            )
        }
    }

    private fun buildCandidates(
        libraryEntries: List<Entry>,
        memberships: List<EntryMergeMembershipSnapshot>,
        counts: Map<Long, Long>,
    ): List<DuplicateLibraryCandidate<Entry>> {
        val entriesById = libraryEntries.associateBy(Entry::id)
        val membershipByEntry = memberships
            .flatMap { membership -> membership.orderedEntryIds.map { it to membership } }
            .toMap()
        val consumed = mutableSetOf<Long>()
        return libraryEntries.mapNotNull { entry ->
            if (!consumed.add(entry.id)) return@mapNotNull null
            val membership = membershipByEntry[entry.id]
            val members = membership?.orderedEntryIds.orEmpty().mapNotNull(entriesById::get)
            if (members.size > 1) {
                consumed += members.map(Entry::id)
                val target = entriesById[membership!!.targetEntryId] ?: members.first()
                val total = members.sumOf { counts[it.id] ?: 0L }
                DuplicateLibraryCandidate(
                    item = target,
                    sortTitle = target.displayTitle,
                    memberIds = members.map(Entry::id),
                    memberEntries = members.map { it.toMetadata(total) },
                    count = total,
                    contentSignature = target.lastModifiedAt,
                )
            } else {
                val count = counts[entry.id] ?: 0L
                DuplicateLibraryCandidate(
                    item = entry,
                    sortTitle = entry.displayTitle,
                    memberIds = listOf(entry.id),
                    memberEntries = listOf(entry.toMetadata(count)),
                    count = count,
                    contentSignature = entry.lastModifiedAt,
                )
            }
        }
    }

    private fun Entry.toMetadata(count: Long?): DuplicateEntryMetadata {
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
}
