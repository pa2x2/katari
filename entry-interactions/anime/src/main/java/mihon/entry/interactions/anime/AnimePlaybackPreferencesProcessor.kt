package mihon.entry.interactions.anime

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryPlaybackPreferencesProcessor
import mihon.entry.interactions.EntryPlaybackPreferencesSnapshot
import mihon.entry.interactions.EntryPlaybackQualityMode
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.PlaybackPreferences
import tachiyomi.domain.entry.model.PlayerQualityMode
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository

internal class AnimePlaybackPreferencesProcessor(
    private val playbackPreferencesRepository: PlaybackPreferencesRepository,
) : EntryPlaybackPreferencesProcessor {
    override val type: EntryType = EntryType.ANIME

    override suspend fun snapshot(entry: Entry): EntryPlaybackPreferencesSnapshot? {
        entry.requireAnime()
        return playbackPreferencesRepository.getByEntryId(entry.id)?.toSnapshot()
    }

    override suspend fun restore(entry: Entry, snapshot: EntryPlaybackPreferencesSnapshot) {
        entry.requireAnime()
        playbackPreferencesRepository.upsert(snapshot.toPlaybackPreferences(entry.id))
    }

    override suspend fun copy(sourceEntry: Entry, targetEntry: Entry) {
        sourceEntry.requireAnime()
        targetEntry.requireAnime()

        playbackPreferencesRepository.getByEntryId(sourceEntry.id)?.let { preferences ->
            playbackPreferencesRepository.upsert(preferences.copy(entryId = targetEntry.id))
        }
    }
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
