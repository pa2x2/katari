package tachiyomi.data.entry

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.entry.model.PlaybackPreferences
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository

class PlaybackPreferencesRepositoryImpl(
    private val handler: DatabaseHandler,
) : PlaybackPreferencesRepository {

    override suspend fun getByEntryId(entryId: Long): PlaybackPreferences? {
        return handler.awaitOneOrNull {
            playback_preferencesQueries.getByEntryId(
                entryId,
                PlaybackPreferencesMapper::mapPreferences,
            )
        }
    }

    override fun getByEntryIdAsFlow(entryId: Long): Flow<PlaybackPreferences?> {
        return handler.subscribeToOneOrNull {
            playback_preferencesQueries.getByEntryId(
                entryId,
                PlaybackPreferencesMapper::mapPreferences,
            )
        }
    }

    override suspend fun upsert(preferences: PlaybackPreferences) {
        handler.await(inTransaction = true) {
            playback_preferencesQueries.upsertUpdate(
                dubKey = preferences.dubKey,
                streamKey = preferences.streamKey,
                sourceQualityKey = preferences.sourceQualityKey,
                subtitleKey = preferences.subtitleKey,
                playerQualityMode = PlaybackPreferencesMapper.encodePlayerQualityMode(
                    preferences.playerQualityMode,
                ),
                playerQualityHeight = preferences.playerQualityHeight?.toLong(),
                subtitleOffsetX = preferences.subtitleOffsetX,
                subtitleOffsetY = preferences.subtitleOffsetY,
                subtitleTextSize = preferences.subtitleTextSize,
                subtitleTextColor = preferences.subtitleTextColor?.toLong(),
                subtitleBackgroundColor = preferences.subtitleBackgroundColor?.toLong(),
                subtitleBackgroundOpacity = preferences.subtitleBackgroundOpacity,
                updatedAt = preferences.updatedAt,
                entryId = preferences.entryId,
            )
            playback_preferencesQueries.upsertInsert(
                entryId = preferences.entryId,
                dubKey = preferences.dubKey,
                streamKey = preferences.streamKey,
                sourceQualityKey = preferences.sourceQualityKey,
                subtitleKey = preferences.subtitleKey,
                playerQualityMode = PlaybackPreferencesMapper.encodePlayerQualityMode(
                    preferences.playerQualityMode,
                ),
                playerQualityHeight = preferences.playerQualityHeight?.toLong(),
                subtitleOffsetX = preferences.subtitleOffsetX,
                subtitleOffsetY = preferences.subtitleOffsetY,
                subtitleTextSize = preferences.subtitleTextSize,
                subtitleTextColor = preferences.subtitleTextColor?.toLong(),
                subtitleBackgroundColor = preferences.subtitleBackgroundColor?.toLong(),
                subtitleBackgroundOpacity = preferences.subtitleBackgroundOpacity,
                updatedAt = preferences.updatedAt,
            )
        }
    }
}
