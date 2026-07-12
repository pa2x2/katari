package eu.kanade.tachiyomi.ui.video.player

import android.app.Application
import android.net.Uri
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryPageResult
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.PlaybackDescriptor
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.SubtitleSource
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.entry.VideoRequest
import eu.kanade.tachiyomi.source.entry.VideoStream
import eu.kanade.tachiyomi.source.entry.VideoStreamType
import eu.kanade.tachiyomi.source.entry.VideoSubtitle
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mihon.entry.interactions.anime.download.AnimeDownloadProvider
import mihon.entry.interactions.anime.download.AnimeDownloader
import mihon.entry.interactions.anime.download.model.AnimeDownloadManifest
import mihon.entry.interactions.anime.download.model.DownloadedSubtitle
import mihon.entry.interactions.anime.download.model.DownloadedVideo
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.PlaybackPreferences
import tachiyomi.domain.entry.model.PlayerQualityMode
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository
import tachiyomi.domain.source.service.SourceManager
import java.io.ByteArrayInputStream

class ResolveVideoStreamTest {

    @Test
    fun `SubtitleSource subtitles are included in successful playback results`() = runTest {
        val playbackSelection = PlaybackSelection(
            dubKey = "resolved-dub",
            sourceQualityKey = "resolved-quality",
        )
        val source = TestSubtitleSource(
            descriptor = playbackDescriptor(selection = playbackSelection),
            subtitles = listOf(
                VideoSubtitle(
                    request = VideoRequest("https://cdn.example.com/subs/en.vtt"),
                    label = "English",
                    language = "en",
                    mimeType = "text/vtt",
                    key = "en",
                    isDefault = true,
                ),
            ),
        )
        val resolver = resolver(source)

        val result = resolver(entryId = ENTRY_ID, chapterId = CHAPTER_ID, ownerEntryId = ENTRY_ID, selection = null)

        val success = result as ResolveVideoStream.Result.Success
        success.subtitles.shouldContainExactly(
            VideoSubtitle(
                request = VideoRequest("https://cdn.example.com/subs/en.vtt"),
                label = "English",
                language = "en",
                mimeType = "text/vtt",
                key = "en",
                isDefault = true,
            ),
        )
        source.subtitleSelection shouldBe playbackSelection
    }

    @Test
    fun `blank subtitle urls are filtered`() = runTest {
        val source = TestSubtitleSource(
            subtitles = listOf(
                VideoSubtitle(
                    request = VideoRequest(""),
                    label = "Blank",
                ),
                VideoSubtitle(
                    request = VideoRequest("https://cdn.example.com/subs/es.vtt"),
                    label = "Spanish",
                    language = "es",
                ),
            ),
        )
        val resolver = resolver(source)

        val result = resolver(entryId = ENTRY_ID, chapterId = CHAPTER_ID, ownerEntryId = ENTRY_ID, selection = null)

        val success = result as ResolveVideoStream.Result.Success
        success.subtitles.map { it.label } shouldBe listOf("Spanish")
    }

    @Test
    fun `SubtitleSource failure does not fail stream resolution`() = runTest {
        val source = TestSubtitleSource(subtitleFailure = IllegalStateException("subtitle failure"))
        val resolver = resolver(source)

        val result = resolver(entryId = ENTRY_ID, chapterId = CHAPTER_ID, ownerEntryId = ENTRY_ID, selection = null)

        val success = result as ResolveVideoStream.Result.Success
        success.subtitles shouldBe emptyList()
        success.stream.request.url shouldBe "https://cdn.example.com/video.m3u8"
    }

    @Test
    fun `non SubtitleSource playback returns an empty subtitle list`() = runTest {
        val source = TestSource()
        val resolver = resolver(source)

        val result = resolver(entryId = ENTRY_ID, chapterId = CHAPTER_ID, ownerEntryId = ENTRY_ID, selection = null)

        val success = result as ResolveVideoStream.Result.Success
        success.subtitles shouldBe emptyList()
    }

    @Test
    fun `resolved fallback selection is persisted`() = runTest {
        val preferences = playbackPreferences(ENTRY_ID).copy(
            dubKey = "requested-dub",
            streamKey = "missing-stream",
            sourceQualityKey = "requested-quality",
        )
        val preferencesRepository = playbackPreferencesRepository(preferences)
        val source = TestSource(
            descriptor = playbackDescriptor(
                selection = PlaybackSelection(
                    dubKey = "fallback-dub",
                    sourceQualityKey = "fallback-quality",
                ),
            ),
        )
        val resolver = resolver(source, preferencesRepository)

        val result = resolver(entryId = ENTRY_ID, chapterId = CHAPTER_ID, ownerEntryId = ENTRY_ID, selection = null)

        val success = result as ResolveVideoStream.Result.Success
        success.playbackData.selection shouldBe PlaybackSelection(
            dubKey = "fallback-dub",
            streamKey = "auto",
            sourceQualityKey = "fallback-quality",
        )
        success.savedPreferences.dubKey shouldBe "fallback-dub"
        success.savedPreferences.streamKey shouldBe "auto"
        success.savedPreferences.sourceQualityKey shouldBe "fallback-quality"
        coVerify(exactly = 1) {
            preferencesRepository.upsert(
                match {
                    it.entryId == ENTRY_ID &&
                        it.dubKey == "fallback-dub" &&
                        it.streamKey == "auto" &&
                        it.sourceQualityKey == "fallback-quality"
                },
            )
        }
    }

    @Test
    fun `unchanged resolved selection keeps saved preferences without persisting`() = runTest {
        val preferences = playbackPreferences(ENTRY_ID).copy(
            streamKey = "auto",
            updatedAt = 123L,
        )
        val preferencesRepository = playbackPreferencesRepository(preferences)
        val resolver = resolver(TestSource(), preferencesRepository)

        val result = resolver(entryId = ENTRY_ID, chapterId = CHAPTER_ID, ownerEntryId = ENTRY_ID, selection = null)

        val success = result as ResolveVideoStream.Result.Success
        success.savedPreferences shouldBe preferences
        coVerify(exactly = 0) { preferencesRepository.upsert(any()) }
    }

    @Test
    fun `downloaded manifest plays offline before source initialization and preserves subtitles`() = runTest {
        val visibleEntry = entry(ENTRY_ID, sourceId = VISIBLE_SOURCE_ID).copy(title = "Visible")
        val ownerEntry = entry(OWNER_ENTRY_ID, sourceId = OWNER_SOURCE_ID).copy(title = "Owner")
        val episode = chapter(CHAPTER_ID, OWNER_ENTRY_ID)
        val visibleSource = TestSource(sourceId = VISIBLE_SOURCE_ID)
        val ownerSource = TestSource(sourceId = OWNER_SOURCE_ID)
        val episodeDir = mockk<UniFile>()
        val manifestFile = mockk<UniFile>()
        val videoFile = localFile("content://downloads/video.mp4", "video.mp4")
        val subtitleFile = localFile("content://downloads/english.vtt", "english.vtt")
        val manifest = AnimeDownloadManifest(
            animeId = OWNER_ENTRY_ID,
            episodeId = CHAPTER_ID,
            animeTitle = "Owner",
            episodeTitle = episode.name,
            originalEpisodeUrl = episode.url,
            qualityMode = "BALANCED",
            selection = PlaybackSelection(streamKey = "source-stream"),
            video = DownloadedVideo(
                fileName = "video.mp4",
                sourceUrl = "https://cdn.example.com/video.mp4",
                headers = emptyMap(),
                label = "1080p",
                streamType = VideoStreamType.PROGRESSIVE,
                mimeType = "video/mp4",
            ),
            subtitles = listOf(
                DownloadedSubtitle(
                    key = "en",
                    label = "English",
                    language = "en",
                    mimeType = "text/vtt",
                    fileName = "english.vtt",
                    isDefault = true,
                    isForced = false,
                ),
            ),
        )
        every { manifestFile.openInputStream() } returns
            ByteArrayInputStream(Json.encodeToString(manifest).toByteArray())
        every { episodeDir.findFile(AnimeDownloader.MANIFEST_FILE_NAME) } returns manifestFile
        every { episodeDir.findFile("video.mp4") } returns videoFile
        every { episodeDir.findFile("english.vtt") } returns subtitleFile
        val provider = mockk<AnimeDownloadProvider> {
            every { findEpisodeDir(any(), any(), any(), any()) } returns null
            every {
                findEpisodeDir(episode.name, episode.url, "Visible", ownerSource)
            } returns episodeDir
        }
        val sourceManager = sourceManager(
            initialized = false,
            VISIBLE_SOURCE_ID to visibleSource,
            OWNER_SOURCE_ID to ownerSource,
        )
        val resolver = resolver(
            visibleEntry = visibleEntry,
            ownerEntry = ownerEntry,
            episode = episode,
            sourceManager = sourceManager,
            provider = provider,
            isOnline = false,
            playbackPreferences = playbackPreferences(OWNER_ENTRY_ID).copy(
                subtitleKey = "en",
                subtitleTextSize = 24.0,
            ),
        )

        val result = resolver(ENTRY_ID, CHAPTER_ID, OWNER_ENTRY_ID, null)

        val success = result as ResolveVideoStream.Result.Success
        success.stream.request.url shouldBe videoFile.uri.toString()
        success.stream.key shouldBe "source-stream"
        success.subtitles.map { it.request.url } shouldBe listOf(subtitleFile.uri.toString())
        success.subtitles.map { it.label } shouldBe listOf("English")
        success.savedPreferences.subtitleKey shouldBe "en"
        success.savedPreferences.subtitleTextSize shouldBe 24.0
        visibleSource.mediaRequests shouldBe 0
        ownerSource.mediaRequests shouldBe 0
        verify { sourceManager.get(VISIBLE_SOURCE_ID) }
        verify { sourceManager.get(OWNER_SOURCE_ID) }
    }

    @Test
    fun `download without manifest uses legacy video file fallback`() = runTest {
        val source = TestSource()
        val episode = chapter(CHAPTER_ID, ENTRY_ID)
        val episodeDir = mockk<UniFile>()
        val videoFile = localFile("content://downloads/legacy.mkv", "legacy.MKV")
        every { episodeDir.findFile(AnimeDownloader.MANIFEST_FILE_NAME) } returns null
        every { episodeDir.listFiles() } returns arrayOf(videoFile)
        val provider = mockk<AnimeDownloadProvider> {
            every { findEpisodeDir(episode.name, episode.url, "Entry 1", source) } returns episodeDir
        }
        val resolver = resolver(
            visibleEntry = entry(ENTRY_ID),
            ownerEntry = entry(ENTRY_ID),
            episode = episode,
            sourceManager = sourceManager(initialized = false, SOURCE_ID to source),
            provider = provider,
            isOnline = false,
        )

        val result = resolver(ENTRY_ID, CHAPTER_ID, ENTRY_ID, null)

        val success = result as ResolveVideoStream.Result.Success
        success.stream.request.url shouldBe videoFile.uri.toString()
        success.stream.type shouldBe VideoStreamType.PROGRESSIVE
        success.stream.key shouldBe "downloaded"
        success.subtitles shouldBe emptyList()
        source.mediaRequests shouldBe 0
    }

    private fun resolver(
        source: UnifiedSource,
        preferencesRepository: PlaybackPreferencesRepository = playbackPreferencesRepository(),
    ): ResolveVideoStream {
        return ResolveVideoStream(
            videoRepository = entryRepository(entry(ENTRY_ID)),
            entryChapterRepository = chapterRepository(chapter(CHAPTER_ID, ENTRY_ID)),
            playbackPreferencesRepository = preferencesRepository,
            videoSourceManager = sourceManager(source),
            animeDownloadProvider = mockk {
                every { findEpisodeDir(any(), any(), any(), any()) } returns null
            },
            json = Json,
            context = mockk<Application>(relaxed = true),
            isOnline = { true },
        )
    }

    private fun resolver(
        visibleEntry: Entry,
        ownerEntry: Entry,
        episode: EntryChapter,
        sourceManager: SourceManager,
        provider: AnimeDownloadProvider,
        isOnline: Boolean,
        playbackPreferences: PlaybackPreferences? = null,
    ): ResolveVideoStream {
        return ResolveVideoStream(
            videoRepository = entryRepository(visibleEntry, ownerEntry),
            entryChapterRepository = chapterRepository(episode),
            playbackPreferencesRepository = playbackPreferencesRepository(playbackPreferences),
            videoSourceManager = sourceManager,
            animeDownloadProvider = provider,
            json = Json,
            context = mockk<Application>(relaxed = true),
            isOnline = { isOnline },
        )
    }

    private fun entryRepository(vararg entries: Entry): EntryRepository {
        val entriesById = entries.associateBy(Entry::id)
        return mockk {
            coEvery { getEntryById(any()) } answers { entriesById[firstArg<Long>()] }
        }
    }

    private fun chapterRepository(vararg chapters: EntryChapter): EntryChapterRepository {
        val chaptersById = chapters.associateBy(EntryChapter::id)
        return mockk {
            coEvery { getChapterById(any()) } answers { chaptersById[firstArg<Long>()] }
        }
    }

    private fun playbackPreferencesRepository(preferences: PlaybackPreferences? = null): PlaybackPreferencesRepository {
        return mockk {
            coEvery { getByEntryId(any()) } returns preferences
            coEvery { upsert(any()) } just runs
        }
    }

    private fun sourceManager(source: UnifiedSource): SourceManager {
        return sourceManager(initialized = true, SOURCE_ID to source)
    }

    private fun sourceManager(
        initialized: Boolean,
        vararg sources: Pair<Long, UnifiedSource>,
    ): SourceManager {
        val sourcesById = sources.toMap()
        val manager = mockk<SourceManager>()
        every { manager.isInitialized } returns MutableStateFlow(initialized)
        every { manager.get(any()) } answers { sourcesById[firstArg<Long>()] }
        return manager
    }

    private fun localFile(uriString: String, fileName: String): UniFile {
        val uri = mockk<Uri>(name = uriString, relaxed = true)
        val file = mockk<UniFile>()
        every { file.uri } returns uri
        every { file.name } returns fileName
        return file
    }

    private open class TestSource(
        private val descriptor: PlaybackDescriptor = playbackDescriptor(),
        sourceId: Long = SOURCE_ID,
    ) : UnifiedSource {

        override val id: Long = sourceId
        override val name: String = "Test Source"
        var mediaSelection: PlaybackSelection? = null
        var mediaRequests: Int = 0

        override suspend fun getPopularContent(page: Int): EntryPageResult<SEntry> = unused()

        override suspend fun getLatestUpdates(page: Int): EntryPageResult<SEntry> = unused()

        override suspend fun getSearchContent(
            page: Int,
            query: String,
            filters: EntryFilterList,
        ): EntryPageResult<SEntry> = unused()

        override suspend fun getContentDetails(entry: SEntry): SEntry = unused()

        override suspend fun getChapterList(entry: SEntry): List<SEntryChapter> = unused()

        override suspend fun getMedia(
            chapter: SEntryChapter,
            selection: PlaybackSelection,
        ): EntryMedia {
            mediaRequests++
            mediaSelection = selection
            return EntryMedia.Playback(descriptor)
        }

        protected fun unused(): Nothing = error("Unused in ResolveVideoStreamTest")
    }

    private class TestSubtitleSource(
        descriptor: PlaybackDescriptor = playbackDescriptor(),
        private val subtitles: List<VideoSubtitle> = emptyList(),
        private val subtitleFailure: Throwable? = null,
    ) : TestSource(descriptor), SubtitleSource {

        var subtitleSelection: PlaybackSelection? = null

        override suspend fun getSubtitles(
            chapter: SEntryChapter,
            selection: PlaybackSelection,
        ): List<VideoSubtitle> {
            subtitleSelection = selection
            subtitleFailure?.let { throw it }
            return subtitles
        }
    }

    private companion object {
        const val ENTRY_ID = 1L
        const val CHAPTER_ID = 2L
        const val OWNER_ENTRY_ID = 3L
        const val SOURCE_ID = 99L
        const val VISIBLE_SOURCE_ID = 100L
        const val OWNER_SOURCE_ID = 101L

        fun playbackDescriptor(
            selection: PlaybackSelection = PlaybackSelection(),
        ): PlaybackDescriptor {
            return PlaybackDescriptor(
                selection = selection,
                streams = listOf(
                    VideoStream(
                        request = VideoRequest("https://cdn.example.com/video.m3u8"),
                        label = "Auto",
                        type = VideoStreamType.HLS,
                        key = "auto",
                    ),
                ),
            )
        }

        fun entry(id: Long, sourceId: Long = SOURCE_ID): Entry {
            return Entry.create().copy(
                id = id,
                source = sourceId,
                title = "Entry $id",
                type = EntryType.ANIME,
                initialized = true,
            )
        }

        fun chapter(id: Long, entryId: Long): EntryChapter {
            return EntryChapter.create().copy(
                id = id,
                entryId = entryId,
                url = "/episode/$id",
                name = "Episode $id",
            )
        }

        fun playbackPreferences(entryId: Long): PlaybackPreferences = PlaybackPreferences(
            entryId = entryId,
            dubKey = null,
            streamKey = null,
            sourceQualityKey = null,
            subtitleKey = null,
            playerQualityMode = PlayerQualityMode.AUTO,
            playerQualityHeight = null,
            subtitleOffsetX = null,
            subtitleOffsetY = null,
            subtitleTextSize = null,
            subtitleTextColor = null,
            subtitleBackgroundColor = null,
            subtitleBackgroundOpacity = null,
            updatedAt = 0L,
        )
    }
}
