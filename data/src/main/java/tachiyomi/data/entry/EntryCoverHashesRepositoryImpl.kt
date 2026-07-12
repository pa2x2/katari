package tachiyomi.data.entry

import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.entry.repository.EntryCoverHashesRepository

class EntryCoverHashesRepositoryImpl(
    private val handler: DatabaseHandler,
    private val profileProvider: ActiveProfileProvider,
) : EntryCoverHashesRepository {

    override suspend fun getCoverHash(entryId: Long, coverLastModified: Long): Long? {
        return handler.awaitOneOrNull {
            entry_cover_hashesQueries.getCoverHash(
                entryId = entryId,
                profileId = profileProvider.activeProfileId,
                coverLastModified = coverLastModified,
            )
        }
    }

    override suspend fun upsertCoverHash(entryId: Long, coverLastModified: Long, hash: Long) {
        handler.await {
            entry_cover_hashesQueries.upsertCoverHash(
                entryId = entryId,
                profileId = profileProvider.activeProfileId,
                coverLastModified = coverLastModified,
                hash = hash,
            )
        }
    }
}
