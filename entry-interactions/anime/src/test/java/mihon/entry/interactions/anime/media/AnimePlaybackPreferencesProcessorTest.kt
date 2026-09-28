package mihon.entry.interactions.anime.media

import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.PlaybackPreferences
import tachiyomi.domain.entry.model.PlayerQualityMode
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository

class AnimePlaybackPreferencesProcessorTest {

    @Test
    fun `snapshot and restore carry every playback preference to the restored entry`() = runTest {
        val sourceEntry = entry(1L)
        val targetEntry = entry(2L)
        val repository = mockk<PlaybackPreferencesRepository>()
        coEvery { repository.getByEntryId(sourceEntry.id) } returns preferences(sourceEntry.id)
        coEvery { repository.upsert(any()) } returns Unit
        val processor = AnimePlaybackPreferencesProcessor(repository)

        processor.restore(targetEntry, processor.snapshot(sourceEntry)!!)

        coVerify(exactly = 1) { repository.upsert(preferences(targetEntry.id)) }
    }

    private fun entry(id: Long): Entry = Entry.create().copy(id = id, type = EntryType.ANIME)

    private fun preferences(entryId: Long): PlaybackPreferences {
        return PlaybackPreferences(
            entryId = entryId,
            dubKey = "dub",
            streamKey = "stream",
            sourceQualityKey = "source-quality",
            subtitleKey = "subtitle",
            playerQualityMode = PlayerQualityMode.SPECIFIC_HEIGHT,
            playerQualityHeight = 1080,
            subtitleOffsetX = 0.1,
            subtitleOffsetY = 0.2,
            subtitleTextSize = 1.3,
            subtitleTextColor = 0xFFFFFF,
            subtitleBackgroundColor = 0,
            subtitleBackgroundOpacity = 0.4,
            updatedAt = 99L,
        )
    }
}
