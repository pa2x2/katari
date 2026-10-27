package tachiyomi.data.entry

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.entry.model.PlaybackPreferences
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository

@Inject
@SingleIn(AppScope::class)
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
        val framing = PlaybackPreferencesMapper.encodePictureFraming(preferences.pictureFraming)
        handler.await(inTransaction = true) {
            playback_preferencesQueries.upsertUpdate(
                dubKey = preferences.dubKey,
                serverKey = preferences.serverKey,
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
                subtitleTextEdge = PlaybackPreferencesMapper.encodeSubtitleTextEdge(preferences.subtitleTextEdge),
                playerEngine = PlaybackPreferencesMapper.encodePlayerEngine(preferences.playerEngine),
                mpvHintShown = preferences.mpvHintShown,
                pictureFraming = framing.name,
                pictureZoom = framing.zoom,
                picturePanX = framing.panX,
                picturePanY = framing.panY,
                updatedAt = preferences.updatedAt,
                entryId = preferences.entryId,
            )
            playback_preferencesQueries.upsertInsert(
                entryId = preferences.entryId,
                dubKey = preferences.dubKey,
                serverKey = preferences.serverKey,
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
                subtitleTextEdge = PlaybackPreferencesMapper.encodeSubtitleTextEdge(preferences.subtitleTextEdge),
                playerEngine = PlaybackPreferencesMapper.encodePlayerEngine(preferences.playerEngine),
                mpvHintShown = preferences.mpvHintShown,
                pictureFraming = framing.name,
                pictureZoom = framing.zoom,
                picturePanX = framing.panX,
                picturePanY = framing.panY,
                updatedAt = preferences.updatedAt,
            )
        }
    }
}
