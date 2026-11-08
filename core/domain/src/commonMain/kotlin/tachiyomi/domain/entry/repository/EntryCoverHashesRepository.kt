package tachiyomi.domain.entry.repository

interface EntryCoverHashesRepository {

    suspend fun getCoverHash(entryId: Long, coverLastModified: Long): Long?

    suspend fun upsertCoverHash(entryId: Long, coverLastModified: Long, hash: Long)
}
