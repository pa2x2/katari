package mihon.entry.interactions.anime.download

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mihon.entry.interactions.anime.download.model.AnimeDownload
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.DownloadPreferences
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.VideoDownloadQualityMode
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository

class AnimeDownloadStoreTest {

    private val backend = FakeBackend()
    private val entryRepository = mockk<EntryRepository>()
    private val chapterRepository = mockk<EntryChapterRepository>()

    @Test
    fun `restores queue order and preferences for their persisted profiles`() = runTest {
        val profileOneEntry = anime(id = 1L, profileId = 10L)
        val profileTwoEntry = anime(id = 2L, profileId = 20L)
        val first = download(profileTwoEntry, episodeId = 22L, quality = VideoDownloadQualityMode.BEST)
        val second = download(profileOneEntry, episodeId = 11L, quality = VideoDownloadQualityMode.DATA_SAVING)
        store().addAll(listOf(first, second))

        coEvery { entryRepository.getAllEntriesByProfile(10L) } returns listOf(profileOneEntry)
        coEvery { entryRepository.getAllEntriesByProfile(20L) } returns listOf(profileTwoEntry)
        coEvery { chapterRepository.getChapterById(11L) } returns second.episode
        coEvery { chapterRepository.getChapterById(22L) } returns first.episode

        val restored = store().restore()

        restored.map { it.episode.id } shouldContainExactly listOf(22L, 11L)
        restored.map { it.preferences.qualityMode } shouldContainExactly listOf(
            VideoDownloadQualityMode.BEST,
            VideoDownloadQualityMode.DATA_SAVING,
        )
        restored.map { it.anime.profileId } shouldContainExactly listOf(20L, 10L)
        backend.data.keys.toList() shouldContainExactly listOf("22", "11")
        coVerify(exactly = 0) { entryRepository.getEntryById(any()) }
    }

    @Test
    fun `persists removals and rewritten queue order`() = runTest {
        val entry = anime(id = 1L, profileId = 10L)
        val first = download(entry, episodeId = 1L)
        val second = download(entry, episodeId = 2L)
        val third = download(entry, episodeId = 3L)
        val writer = store()
        writer.addAll(listOf(first, second, third))
        writer.remove(second)

        coEvery { entryRepository.getAllEntriesByProfile(10L) } returns listOf(entry)
        coEvery { chapterRepository.getChapterById(1L) } returns first.episode
        coEvery { chapterRepository.getChapterById(3L) } returns third.episode

        store().restore().map { it.episode.id } shouldContainExactly listOf(1L, 3L)

        writer.addAll(listOf(first, third))
        writer.clear()
        writer.addAll(listOf(third, first))
        store().restore().map { it.episode.id } shouldContainExactly listOf(3L, 1L)
    }

    @Test
    fun `drops malformed and stale rows without preventing valid restoration`() = runTest {
        val entry = anime(id = 1L, profileId = 10L)
        val valid = download(entry, episodeId = 1L)
        val stale = download(entry, episodeId = 2L)
        store().addAll(listOf(valid, stale))
        backend.data["malformed"] = "{not json"
        backend.data["wrong type"] = 3L

        coEvery { entryRepository.getAllEntriesByProfile(10L) } returns listOf(entry)
        coEvery { chapterRepository.getChapterById(1L) } returns valid.episode
        coEvery { chapterRepository.getChapterById(2L) } returns null

        val restored = store().restore()

        restored.map { it.episode.id } shouldContainExactly listOf(1L)
        backend.data.keys.toList() shouldContainExactly listOf("1", "2", "malformed", "wrong type")
    }

    @Test
    fun `restores legacy rows through the active profile lookup`() = runTest {
        val entry = anime(id = 5L, profileId = 10L)
        val episode = EntryChapter.create().copy(id = 6L, entryId = entry.id)
        backend.data["6"] =
            """{"animeId":5,"episodeId":6,"dubKey":"dub","streamKey":null,"subtitleKey":null,"qualityMode":"balanced","updatedAt":7,"order":0}"""
        coEvery { entryRepository.getEntryById(5L) } returns entry
        coEvery { chapterRepository.getChapterById(6L) } returns episode

        val restored = store().restore().single()

        restored.anime shouldBe entry
        restored.preferences.entryId shouldBe entry.id
        restored.preferences.dubKey shouldBe "dub"
    }

    private fun store() = AnimeDownloadStore(
        backend = backend,
        json = Json,
        entryRepository = entryRepository,
        entryChapterRepository = chapterRepository,
    )

    private fun anime(id: Long, profileId: Long): Entry = Entry.create().copy(
        id = id,
        profileId = profileId,
        type = EntryType.ANIME,
    )

    private fun download(
        entry: Entry,
        episodeId: Long,
        quality: VideoDownloadQualityMode = VideoDownloadQualityMode.BALANCED,
    ): AnimeDownload {
        return AnimeDownload(
            anime = entry,
            episode = EntryChapter.create().copy(id = episodeId, entryId = entry.id),
            preferences = DownloadPreferences(
                entryId = entry.id,
                dubKey = "dub-$episodeId",
                streamKey = "stream-$episodeId",
                subtitleKey = "subtitle-$episodeId",
                qualityMode = quality,
                updatedAt = episodeId,
            ),
        )
    }

    private class FakeBackend : AnimeDownloadStoreBackend {
        val data = linkedMapOf<String, Any?>()

        override fun values(): Map<String, *> = data.toMap()

        override fun putAll(values: Map<String, String>) {
            data.putAll(values)
        }

        override fun remove(keys: Set<String>) {
            data.keys.removeAll(keys)
        }

        override fun clear() {
            data.clear()
        }
    }
}
