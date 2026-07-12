package tachiyomi.data.entry

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.entry.model.DownloadPreferences
import tachiyomi.domain.entry.repository.DownloadPreferencesRepository

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
                streamKey = preferences.streamKey,
                subtitleKey = preferences.subtitleKey,
                qualityMode = DownloadPreferencesMapper.encodeQualityMode(preferences.qualityMode),
                updatedAt = preferences.updatedAt,
                entryId = preferences.entryId,
            )
            download_preferencesQueries.upsertInsert(
                entryId = preferences.entryId,
                dubKey = preferences.dubKey,
                streamKey = preferences.streamKey,
                subtitleKey = preferences.subtitleKey,
                qualityMode = DownloadPreferencesMapper.encodeQualityMode(preferences.qualityMode),
                updatedAt = preferences.updatedAt,
            )
        }
    }
}
