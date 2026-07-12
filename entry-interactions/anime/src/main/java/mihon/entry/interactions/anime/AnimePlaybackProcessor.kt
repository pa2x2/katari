package mihon.entry.interactions.anime

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.first
import mihon.entry.interactions.EntryPlaybackChapterMapping
import mihon.entry.interactions.EntryPlaybackPreferencesSnapshot
import mihon.entry.interactions.EntryPlaybackProcessor
import mihon.entry.interactions.EntryPlaybackQualityMode
import mihon.entry.interactions.EntryPlaybackSnapshot
import mihon.entry.interactions.EntryPlaybackStateSnapshot
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.PlaybackPreferences
import tachiyomi.domain.entry.model.PlaybackState
import tachiyomi.domain.entry.model.PlayerQualityMode
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository
import tachiyomi.domain.entry.repository.PlaybackStateRepository

internal class AnimePlaybackProcessor(
    private val playbackStateRepository: PlaybackStateRepository,
    private val playbackPreferencesRepository: PlaybackPreferencesRepository,
) : EntryPlaybackProcessor {
    override val type: EntryType = EntryType.ANIME

    override suspend fun snapshot(entry: Entry): EntryPlaybackSnapshot {
        entry.requireAnime()
        return EntryPlaybackSnapshot(
            states = playbackStateRepository.getByEntryIdAsFlow(entry.id)
                .first()
                .map { it.toSnapshot() },
            preferences = playbackPreferencesRepository.getByEntryId(entry.id)?.toSnapshot(),
        )
    }

    override suspend fun restore(entry: Entry, snapshot: EntryPlaybackSnapshot) {
        entry.requireAnime()
        snapshot.states.forEach { state ->
            playbackStateRepository.upsert(state.toPlaybackState(entry.id))
        }
        snapshot.preferences?.let { preferences ->
            playbackPreferencesRepository.upsert(preferences.toPlaybackPreferences(entry.id))
        }
    }

    override suspend fun copy(
        sourceEntry: Entry,
        targetEntry: Entry,
        chapterMappings: List<EntryPlaybackChapterMapping>,
    ) {
        sourceEntry.requireAnime()
        targetEntry.requireAnime()

        val targetChapterIdBySourceChapterId = chapterMappings.associate {
            it.sourceChapterId to it.targetChapterId
        }
        val statesBySourceChapterId = playbackStateRepository.getByEntryIdAsFlow(sourceEntry.id)
            .first()
            .associateBy { it.chapterId }

        targetChapterIdBySourceChapterId.forEach { (sourceChapterId, targetChapterId) ->
            val state = statesBySourceChapterId[sourceChapterId] ?: return@forEach
            playbackStateRepository.upsert(
                state.copy(
                    entryId = targetEntry.id,
                    chapterId = targetChapterId,
                ),
            )
        }

        playbackPreferencesRepository.getByEntryId(sourceEntry.id)?.let { preferences ->
            playbackPreferencesRepository.upsert(preferences.copy(entryId = targetEntry.id))
        }
    }
}

private fun PlaybackState.toSnapshot(): EntryPlaybackStateSnapshot {
    return EntryPlaybackStateSnapshot(
        chapterId = chapterId,
        positionMs = positionMs,
        durationMs = durationMs,
        completed = completed,
        lastWatchedAt = lastWatchedAt,
    )
}

private fun EntryPlaybackStateSnapshot.toPlaybackState(entryId: Long): PlaybackState {
    return PlaybackState(
        entryId = entryId,
        chapterId = chapterId,
        positionMs = positionMs,
        durationMs = durationMs,
        completed = completed,
        lastWatchedAt = lastWatchedAt,
    )
}

private fun PlaybackPreferences.toSnapshot(): EntryPlaybackPreferencesSnapshot {
    return EntryPlaybackPreferencesSnapshot(
        dubKey = dubKey,
        streamKey = streamKey,
        sourceQualityKey = sourceQualityKey,
        subtitleKey = subtitleKey,
        playerQualityMode = playerQualityMode.toSnapshotMode(),
        playerQualityHeight = playerQualityHeight,
        subtitleOffsetX = subtitleOffsetX,
        subtitleOffsetY = subtitleOffsetY,
        subtitleTextSize = subtitleTextSize,
        subtitleTextColor = subtitleTextColor,
        subtitleBackgroundColor = subtitleBackgroundColor,
        subtitleBackgroundOpacity = subtitleBackgroundOpacity,
        updatedAt = updatedAt,
    )
}

private fun EntryPlaybackPreferencesSnapshot.toPlaybackPreferences(entryId: Long): PlaybackPreferences {
    return PlaybackPreferences(
        entryId = entryId,
        dubKey = dubKey,
        streamKey = streamKey,
        sourceQualityKey = sourceQualityKey,
        subtitleKey = subtitleKey,
        playerQualityMode = playerQualityMode.toDomainMode(),
        playerQualityHeight = playerQualityHeight,
        subtitleOffsetX = subtitleOffsetX,
        subtitleOffsetY = subtitleOffsetY,
        subtitleTextSize = subtitleTextSize,
        subtitleTextColor = subtitleTextColor,
        subtitleBackgroundColor = subtitleBackgroundColor,
        subtitleBackgroundOpacity = subtitleBackgroundOpacity,
        updatedAt = updatedAt,
    )
}

private fun PlayerQualityMode.toSnapshotMode(): EntryPlaybackQualityMode {
    return when (this) {
        PlayerQualityMode.AUTO -> EntryPlaybackQualityMode.AUTO
        PlayerQualityMode.SPECIFIC_HEIGHT -> EntryPlaybackQualityMode.SPECIFIC_HEIGHT
    }
}

private fun EntryPlaybackQualityMode.toDomainMode(): PlayerQualityMode {
    return when (this) {
        EntryPlaybackQualityMode.AUTO -> PlayerQualityMode.AUTO
        EntryPlaybackQualityMode.SPECIFIC_HEIGHT -> PlayerQualityMode.SPECIFIC_HEIGHT
    }
}
