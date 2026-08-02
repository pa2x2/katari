package mihon.feature.migration.review

import mihon.feature.migration.session.model.SourceMigrationCandidate
import mihon.feature.migration.session.model.SourceMigrationSession
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository

internal class SourceMigrationCandidateEntryResolver(
    private val entryRepository: EntryRepository,
    private val networkToLocalEntry: NetworkToLocalEntry,
) {
    suspend fun resolve(
        session: SourceMigrationSession,
        sourceEntryId: Long,
        candidate: SourceMigrationCandidate,
    ): SourceMigrationResolvedCandidate? {
        if (candidate.sessionId != session.id || candidate.sourceEntryId != sourceEntryId) return null
        val source = entryRepository.getEntryById(sourceEntryId, session.profileId) ?: return null
        val target = networkToLocalEntry(
            Entry.create().copy(
                source = candidate.targetSourceId,
                url = candidate.targetUrl,
                title = candidate.targetTitle,
                thumbnailUrl = candidate.targetThumbnailUrl,
                type = source.type,
                profileId = session.profileId,
            ),
            session.profileId,
        )
        return SourceMigrationResolvedCandidate(source = source, target = target)
    }

    suspend fun reloadTarget(target: Entry, profileId: Long): Entry {
        return entryRepository.getEntryById(target.id, profileId) ?: target
    }
}

internal data class SourceMigrationResolvedCandidate(
    val source: Entry,
    val target: Entry,
)
