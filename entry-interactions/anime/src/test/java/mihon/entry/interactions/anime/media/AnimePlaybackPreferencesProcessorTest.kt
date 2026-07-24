package mihon.entry.interactions.anime

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.EntryPlaybackPreferencesSnapshot
import mihon.entry.interactions.EntryPlaybackQualityMode
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.PlaybackPreferences
import tachiyomi.domain.entry.model.PlayerQualityMode
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository

class AnimePlaybackPreferencesProcessorTest {
    private val sourceEntry = entry(1L)
    private val targetEntry = entry(2L)

    @Test
    fun `snapshot and restore translate the complete playback preference model`() = runTest {
        val repository = mockk<PlaybackPreferencesRepository>()
        val preferences = preferences(sourceEntry.id)
        coEvery { repository.getByEntryId(sourceEntry.id) } returns preferences
        coEvery { repository.upsert(any()) } returns Unit
        val processor = AnimePlaybackPreferencesProcessor(repository)

        val snapshot = processor.snapshot(sourceEntry)
        snapshot shouldBe EntryPlaybackPreferencesSnapshot(
            dubKey = "dub",
            streamKey = "stream",
            sourceQualityKey = "source-quality",
            subtitleKey = "subtitle",
            playerQualityMode = EntryPlaybackQualityMode.SPECIFIC_HEIGHT,
            playerQualityHeight = 1080,
            subtitleOffsetX = 0.1,
            subtitleOffsetY = 0.2,
            subtitleTextSize = 1.3,
            subtitleTextColor = 0xFFFFFF,
            subtitleBackgroundColor = 0,
            subtitleBackgroundOpacity = 0.4,
            updatedAt = 99L,
        )

        processor.restore(targetEntry, snapshot!!)
        coVerify(exactly = 1) { repository.upsert(preferences(targetEntry.id)) }
    }

    @Test
    fun `copy reports whether source preferences existed`() = runTest {
        val repository = mockk<PlaybackPreferencesRepository>()
        val preferences = preferences(sourceEntry.id)
        coEvery { repository.getByEntryId(sourceEntry.id) } returnsMany listOf(preferences, null)
        coEvery { repository.upsert(any()) } returns Unit
        val processor = AnimePlaybackPreferencesProcessor(repository)

        processor.copy(sourceEntry, targetEntry) shouldBe true
        processor.copy(sourceEntry, targetEntry) shouldBe false

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
