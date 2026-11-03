package eu.kanade.tachiyomi.ui.video.player

import android.app.Application
import android.net.Uri
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryPageResult
import eu.kanade.tachiyomi.source.entry.EntryRefresh
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.PlaybackDescriptor
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.entry.VideoRequest
import eu.kanade.tachiyomi.source.entry.VideoStream
import eu.kanade.tachiyomi.source.entry.VideoStreamType
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.anime.download.AnimeDownloadProvider
import mihon.entry.interactions.anime.download.model.AnimeDownloadManifest
import mihon.entry.interactions.anime.download.model.DownloadedSubtitle
import mihon.entry.interactions.anime.download.model.DownloadedVideo
import mihon.entry.interactions.anime.download.model.EpisodePackage
import mihon.entry.interactions.anime.stream.StreamTypeProbe
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test
import tachiyomi.core.common.storage.ListedFile
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.PlaybackPreferences
import tachiyomi.domain.entry.model.PlayerQualityMode
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository
import tachiyomi.domain.source.model.UnifiedStubSource
import tachiyomi.domain.source.service.SourceManager

class ResolveVideoStreamTest {

    @Test
    fun `downloaded manifest plays offline before source initialization and preserves subtitles`() = runTest {
        val visibleEntry = entry(ENTRY_ID, sourceId = VISIBLE_SOURCE_ID).copy(title = "Visible")
        val ownerEntry = entry(OWNER_ENTRY_ID, sourceId = OWNER_SOURCE_ID).copy(title = "Owner")
        val episode = chapter(CHAPTER_ID, OWNER_ENTRY_ID)
        val visibleSource = TestSource(sourceId = VISIBLE_SOURCE_ID)
        val ownerSource = TestSource(sourceId = OWNER_SOURCE_ID)
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
        val files = listOf(videoFile, subtitleFile).associateBy { checkNotNull(it.name) }
        val provider = mockk<AnimeDownloadProvider> {
            every { findEpisodePackage(any(), any(), any(), any()) } returns null
            every {
                findEpisodePackage(episode.name, episode.url, "Visible", ownerSource)
            } returns EpisodePackage(manifest, files)
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
        success.stream.request?.url shouldBe videoFile.file.uri.toString()
        success.stream.key shouldBe "source-stream"
        success.stream.subtitles.map { it.request.url } shouldBe listOf(subtitleFile.file.uri.toString())
        success.stream.subtitles.map { it.label } shouldBe listOf("English")
        success.preferences.subtitleKey shouldBe "en"
        success.preferences.subtitleTextSize shouldBe 24.0
        visibleSource.mediaRequests shouldBe 0
        ownerSource.mediaRequests shouldBe 0
    }

    @Test
    fun `torrent links are never played and an episode offered only as one says so`() = runTest {
        val episode = chapter(CHAPTER_ID, ENTRY_ID)
        val magnet = VideoStream(request = VideoRequest("magnet:?xt=urn:btih:abc"), label = "4K", key = "magnet")
        val torrentFile = VideoStream(
            request = VideoRequest("https://tracker.example.com/episode.torrent?passkey=1"),
            label = "1080p",
            key = "file",
        )
        val direct =
            VideoStream(request = VideoRequest("https://cdn.example.com/episode.m3u8"), label = "720p", key = "hls")
        val provider = mockk<AnimeDownloadProvider> {
            every { findEpisodePackage(any(), any(), any(), any()) } returns null
        }
        fun resolve(vararg streams: VideoStream) = resolver(
            visibleEntry = entry(ENTRY_ID),
            ownerEntry = entry(ENTRY_ID),
            episode = episode,
            sourceManager = sourceManager(
                initialized = true,
                SOURCE_ID to TestSource(
                    PlaybackDescriptor(selection = PlaybackSelection(), streams = streams.toList()),
                ),
            ),
            provider = provider,
            isOnline = true,
        )

        val onlyTorrents = resolve(magnet, torrentFile)(ENTRY_ID, CHAPTER_ID, ENTRY_ID, null)
        val mixed = resolve(magnet, direct, torrentFile)(ENTRY_ID, CHAPTER_ID, ENTRY_ID, null)

        (onlyTorrents as ResolveVideoStream.Result.Error).reason shouldBe ResolveVideoStream.Reason.TorrentOnly
        (mixed as ResolveVideoStream.Result.Success).rankedStreams.map { it.key } shouldBe listOf("hls")
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
            streamTypeProbe = StreamTypeProbe(OkHttpClient(), defaultUserAgent = { "test" }),
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
        }
    }

    private fun sourceManager(
        initialized: Boolean,
        vararg sources: Pair<Long, UnifiedSource>,
    ): SourceManager {
        val sourcesById = sources.toMap()
        val manager = mockk<SourceManager>()
        every { manager.isInitialized } returns MutableStateFlow(initialized)
        every { manager.get(any()) } answers { sourcesById[firstArg<Long>()] }
        every { manager.getOrStub(any()) } answers {
            sourcesById[firstArg<Long>()] ?: UnifiedStubSource(firstArg(), lang = "", name = "")
        }
        return manager
    }

    private fun localFile(uriString: String, fileName: String): ListedFile {
        val uri = mockk<Uri>(name = uriString, relaxed = true)
        val file = mockk<UniFile>()
        every { file.uri } returns uri
        return ListedFile(file, fileName, isDirectory = false, length = 1L, lastModified = 0L)
    }

    private class TestSource(
        private val descriptor: PlaybackDescriptor = playbackDescriptor(),
        sourceId: Long = SOURCE_ID,
    ) : UnifiedSource {

        override val id: Long = sourceId
        override val name: String = "Test Source"
        var mediaRequests: Int = 0

        override suspend fun getPopularContent(page: Int): EntryPageResult<SEntry> = unused()

        override suspend fun getLatestUpdates(page: Int): EntryPageResult<SEntry> = unused()

        override suspend fun getSearchContent(
            page: Int,
            query: String,
            filters: EntryFilterList,
        ): EntryPageResult<SEntry> = unused()

        override suspend fun refresh(
            entry: SEntry,
            existingChapters: List<SEntryChapter>,
            fetchDetails: Boolean,
            fetchChapters: Boolean,
        ): EntryRefresh = unused()

        override suspend fun getMedia(
            chapter: SEntryChapter,
            selection: PlaybackSelection,
        ): EntryMedia {
            mediaRequests++
            return EntryMedia.Playback(descriptor)
        }

        private fun unused(): Nothing = error("Unused in ResolveVideoStreamTest")
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
