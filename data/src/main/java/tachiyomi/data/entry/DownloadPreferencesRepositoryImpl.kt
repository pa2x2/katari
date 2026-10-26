package tachiyomi.data.entry

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.query.chunkedForSqlQuery
import tachiyomi.domain.entry.model.DownloadPreferences
import tachiyomi.domain.entry.repository.DownloadPreferencesRepository

@Inject
@SingleIn(AppScope::class)
class DownloadPreferencesRepositoryImpl(
    private val handler: DatabaseHandler,
) : DownloadPreferencesRepository {

    override suspend fun getByEntryId(entryId: Long): DownloadPreferences? {
        return handler.awaitOneOrNull {
            download_preferencesQueries.getByEntryId(
                entryId,
                DownloadPreferencesMapper::mapPreferences,
            )
        }
    }

    override suspend fun getByEntryIds(entryIds: Collection<Long>): Map<Long, DownloadPreferences> {
        return entryIds.distinct().chunkedForSqlQuery().flatMap { chunk ->
            handler.awaitList {
                download_preferencesQueries.getByEntryIds(chunk, DownloadPreferencesMapper::mapPreferences)
            }
        }
            .associateBy(DownloadPreferences::entryId)
    }

    override fun getByEntryIdAsFlow(entryId: Long): Flow<DownloadPreferences?> {
        return handler.subscribeToOneOrNull {
            download_preferencesQueries.getByEntryId(
                entryId,
                DownloadPreferencesMapper::mapPreferences,
            )
        }
    }

    override suspend fun upsert(preferences: DownloadPreferences) {
        handler.await(inTransaction = true) {
            download_preferencesQueries.upsertUpdate(
                dubKey = preferences.dubKey,
                serverKey = preferences.serverKey,
                streamKey = preferences.streamKey,
                subtitleKey = preferences.subtitleKey,
                audioKey = preferences.audioKey,
                qualityMode = DownloadPreferencesMapper.encodeQualityMode(preferences.qualityMode),
                updatedAt = preferences.updatedAt,
                entryId = preferences.entryId,
            )
            download_preferencesQueries.upsertInsert(
                entryId = preferences.entryId,
                dubKey = preferences.dubKey,
                serverKey = preferences.serverKey,
                streamKey = preferences.streamKey,
                subtitleKey = preferences.subtitleKey,
                audioKey = preferences.audioKey,
                qualityMode = DownloadPreferencesMapper.encodeQualityMode(preferences.qualityMode),
                updatedAt = preferences.updatedAt,
            )
        }
    }
}
