package mihon.entry.interactions.anime

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryPreviewSource
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.PlaybackDescriptor
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.SubtitleSource
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.entry.VideoPlaybackOption
import eu.kanade.tachiyomi.source.entry.VideoRequest
import eu.kanade.tachiyomi.source.entry.VideoStream
import eu.kanade.tachiyomi.source.entry.VideoSubtitle
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.EntryBulkDownloadAction
import mihon.entry.interactions.EntryBulkDownloadCandidateResult
import mihon.entry.interactions.EntryChildListRequest
import mihon.entry.interactions.EntryChildListRow
import mihon.entry.interactions.EntryChildProgressRequest
import mihon.entry.interactions.EntryDownloadOptionSelection
import mihon.entry.interactions.EntryDownloadState
import mihon.entry.interactions.EntryInteractionPlugin
import mihon.entry.interactions.EntryOpenOptions
import mihon.entry.interactions.EntryPlaybackPreferencesSnapshot
import mihon.entry.interactions.EntryPlaybackQualityMode
import mihon.entry.interactions.EntryProgressResourceMapping
import mihon.entry.interactions.EntryProgressSnapshot
import mihon.entry.interactions.EntryProgressStateSnapshot
import mihon.entry.interactions.anime.download.AnimeDownloadCache
import mihon.entry.interactions.anime.download.AnimeDownloadManager
import mihon.entry.interactions.anime.download.model.AnimeDownload
import mihon.entry.interactions.createEntryInteractions
import mihon.entry.interactions.settings.EntryInteractionPreferences
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.DownloadPreferences
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryMerge
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.model.PlaybackPreferences
import tachiyomi.domain.entry.model.PlayerQualityMode
import tachiyomi.domain.entry.model.VideoDownloadQualityMode
import tachiyomi.domain.entry.repository.DownloadPreferencesRepository
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.MergedEntryRepository
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.ProgressState
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR

class AnimeEntryInteractionPluginTest {
    private val context = mockk<Context>(relaxed = true)

    @Test
    fun `plugin registers anime processors`() = runTest {
        val dependencies = dependencies(
            chapters = listOf(chapter(id = 10L, read = false)),
            episodeDownloaded = true,
        )
        val interactions = createEntryInteractions(listOf(animeEntryInteractionPlugin(dependencies)))
        val entry = entry(EntryType.ANIME, id = 1L)

        val continued = interactions.continueEntry.findNext(entry)
        val status = interactions.download.getStatus(
            entryType = EntryType.ANIME,
            chapterId = 10L,
            chapterName = "Episode",
            chapterScanlator = null,
            chapterUrl = "/episode",
            entryTitle = "Entry",
            sourceId = 1L,
        )

        continued?.id shouldBe 10L
        status.state shouldBe EntryDownloadState.DOWNLOADED
    }

    @Test
    fun `anime bulk downloads select next or all unconsumed episodes`() = runTest {
        val anime = entry(EntryType.ANIME)
        val episodes = (1L..30L).map { chapter(id = it, sourceOrder = it) } +
            chapter(id = 31L, read = true, sourceOrder = 31L)
        val interactions = createEntryInteractions(
            listOf(animeEntryInteractionPlugin(dependencies(chapters = episodes))),
        )

        interactions.download.supportsBulkDownload(anime) shouldBe true
        listOf(1, 5, 10, 25).forEach { limit ->
            val next = interactions.download.resolveBulkDownloadCandidates(
                anime,
                EntryBulkDownloadAction.next(limit),
                candidates = episodes,
            ) as EntryBulkDownloadCandidateResult.Supported
            next.chapters.size shouldBe limit
            next.chapters.all { !it.read } shouldBe true
        }
        val all = interactions.download.resolveBulkDownloadCandidates(
            anime,
            EntryBulkDownloadAction.unread,
            candidates = episodes,
        ) as EntryBulkDownloadCandidateResult.Supported

        all.chapters.size shouldBe 30
        all.chapters.all { !it.read } shouldBe true
    }

    @Test
    fun `anime download options use Entry playback models and selected preferences are persisted`() = runTest {
        val anime = entry(EntryType.ANIME)
        val episode = chapter(id = 2L)
        val savedPreferences = downloadPreferences(anime.id).copy(
            dubKey = "dub-en",
            streamKey = "stream-720",
            subtitleKey = "sub-en",
            qualityMode = VideoDownloadQualityMode.BEST,
        )
        val repository = FakeDownloadPreferencesRepository(savedPreferences)
        val manager = mockAnimeDownloadManager(false)
        val source = mockk<SubtitleSource> {
            every { id } returns anime.source
            every { name } returns "Anime source"
            coEvery { getMedia(any(), any()) } returns EntryMedia.Playback(
                PlaybackDescriptor(
                    selection = PlaybackSelection(dubKey = "dub-en", streamKey = "stream-720"),
                    dubs = listOf(VideoPlaybackOption("dub-en", "English")),
                    streams = listOf(
                        VideoStream(
                            request = VideoRequest("https://cdn.example/video.mp4"),
                            label = "720p",
                            key = "stream-720",
                        ),
                    ),
                ),
            )
            coEvery { getSubtitles(any(), any()) } returns listOf(
                VideoSubtitle(
                    request = VideoRequest("https://cdn.example/sub.vtt"),
                    label = "English",
                    language = "en",
                    key = "sub-en",
                ),
            )
        }
        val interactions = createEntryInteractions(
            listOf(
                animeEntryInteractionPlugin(
                    dependencies(
                        chapters = listOf(episode),
                        animeDownloadManager = manager,
                        downloadPreferencesRepository = repository,
                        sourceManager = mockSourceManager(source),
                    ),
                ),
            ),
        )

        val options = interactions.download.resolveDownloadOptions(context, anime, episode)!!
        val groups = options.groups.associateBy { it.key }

        groups.getValue("quality").selectedKey shouldBe savedPreferences.qualityMode.name
        groups.getValue("dub").options.map { it.key } shouldContainExactly listOf("dub-en")
        groups.getValue("stream").options.map { it.key } shouldContainExactly listOf("stream-720")
        groups.getValue("subtitle").options.map { it.key } shouldContainExactly listOf("sub-en")

        val selected = EntryDownloadOptionSelection(
            mapOf(
                "quality" to VideoDownloadQualityMode.DATA_SAVING.name,
                "dub" to savedPreferences.dubKey,
                "stream" to null,
                "subtitle" to null,
            ),
        )
        interactions.download.downloadWithOptions(anime, listOf(episode), selected, startNow = false)

        repository.upserted.single().copy(updatedAt = 0L) shouldBe savedPreferences.copy(
            streamKey = null,
            subtitleKey = null,
            qualityMode = VideoDownloadQualityMode.DATA_SAVING,
            updatedAt = 0L,
        )
        (repository.upserted.single().updatedAt > 0L) shouldBe true
        verify {
            manager.queueEpisodes(
                anime = anime,
                episodes = listOf(episode),
                preferences = repository.upserted.single(),
                autoStart = false,
            )
        }
    }

    @Test
    fun `anime merged library progress keeps first member continue target`() {
        val calculator = animeEntryLibraryProgressCalculator(FakeEntryProgressRepository(emptyList()))

        val state = calculator.merge(
            listOf(
                libraryItem(entryId = 7L, continueEntryId = null),
                libraryItem(entryId = 8L, continueEntryId = 82L),
            ),
        )

        state.continueEntryId shouldBe 82L
        state.progress.canContinue(state.continueEntryId, unconsumedCount = 1L) shouldBe true
    }

    @Test
    fun `anime child list defaults to chapter number descending without missing chapter ranges`() = runTest {
        val interactions = createEntryInteractions(listOf(animeEntryInteractionPlugin(dependencies())))
        val entry = entry(EntryType.ANIME)
        val rows = interactions.childList.buildDisplayList(
            EntryChildListRequest(
                entry = entry,
                chapters = listOf(
                    chapter(id = 3L, sourceOrder = 30L, chapterNumber = 3.0),
                    chapter(id = 1L, sourceOrder = 10L, chapterNumber = 1.0),
                    chapter(id = 2L, sourceOrder = 20L, chapterNumber = 2.0),
                ),
                memberIds = listOf(entry.id),
                includeMissingCounts = true,
            ),
        )

        rows.filterIsInstance<EntryChildListRow.MissingCount>() shouldBe emptyList()
        rows.filterIsInstance<EntryChildListRow.Child>()
            .map { it.chapter.id }
            .shouldContainExactly(3L, 2L, 1L)
    }

    @Test
    fun `in progress playback state returns timestamp progress`() = runTest {
        val interactions = createEntryInteractions(
            listOf(
                animeEntryInteractionPlugin(
                    dependencies(
                        playbackStates = listOf(
                            playbackState(chapterId = 7L, positionMs = 65_000L, durationMs = 3_665_000L),
                        ),
                    ),
                ),
            ),
        )

        val labels = interactions.childList.progressLabels(
            EntryChildProgressRequest(
                entry = entry(EntryType.ANIME),
                chapters = listOf(chapter(id = 7L, read = false)),
            ),
        ).first()

        labels[7L]?.resource shouldBe MR.strings.episode_progress_timestamp
        labels[7L]?.args shouldBe listOf("1:05", "1:01:05")
    }

    @Test
    fun `unknown duration returns position only progress`() = runTest {
        val interactions = createEntryInteractions(
            listOf(
                animeEntryInteractionPlugin(
                    dependencies(
                        playbackStates = listOf(
                            playbackState(chapterId = 7L, positionMs = 65_000L, durationMs = 0L),
                        ),
                    ),
                ),
            ),
        )

        val labels = interactions.childList.progressLabels(
            EntryChildProgressRequest(
                entry = entry(EntryType.ANIME),
                chapters = listOf(chapter(id = 7L, read = false)),
            ),
        ).first()

        labels[7L]?.resource shouldBe MR.strings.episode_progress_position
        labels[7L]?.args shouldBe listOf("1:05")
    }

    @Test
    fun `completed watched zero missing and legacy page progress do not return labels`() = runTest {
        val interactions = createEntryInteractions(
            listOf(
                animeEntryInteractionPlugin(
                    dependencies(
                        playbackStates = listOf(
                            playbackState(chapterId = 1L, positionMs = 65_000L, completed = true),
                            playbackState(chapterId = 2L, positionMs = 65_000L, completed = false),
                            playbackState(chapterId = 3L, positionMs = 0L, completed = false),
                        ),
                    ),
                ),
            ),
        )

        val labels = interactions.childList.progressLabels(
            EntryChildProgressRequest(
                entry = entry(EntryType.ANIME),
                chapters = listOf(
                    chapter(id = 1L, read = false),
                    chapter(id = 2L, read = true),
                    chapter(id = 3L, read = false),
                    chapter(id = 4L, read = false),
                    chapter(id = 5L, read = false, lastPageRead = 4L),
                ),
            ),
        ).first()

        labels shouldBe emptyMap()
    }

    @Test
    fun `merged anime member ids combine playback states from all members`() = runTest {
        val interactions = createEntryInteractions(
            listOf(
                animeEntryInteractionPlugin(
                    dependencies(
                        playbackStates = listOf(
                            playbackState(entryId = 1L, chapterId = 11L, positionMs = 65_000L),
                            playbackState(entryId = 2L, chapterId = 22L, positionMs = 125_000L),
                        ),
                    ),
                ),
            ),
        )

        val labels = interactions.childList.progressLabels(
            EntryChildProgressRequest(
                entry = entry(EntryType.ANIME, id = 1L),
                chapters = listOf(
                    chapter(id = 11L, entryId = 1L),
                    chapter(id = 22L, entryId = 2L),
                ),
                memberIds = listOf(1L, 2L),
            ),
        ).first()

        labels.keys shouldBe setOf(11L, 22L)
        labels[11L]?.args shouldBe listOf("1:05", "1:00")
        labels[22L]?.args shouldBe listOf("2:05", "1:00")
    }

    @Test
    fun `anime progress and playback preference snapshots stay separate`() = runTest {
        val dependencies = dependencies(
            playbackStates = listOf(
                playbackState(
                    chapterId = 10L,
                    positionMs = 12_000L,
                    durationMs = 60_000L,
                    completed = false,
                    lastWatchedAt = 123L,
                ),
            ),
            playbackPreferences = playbackPreferences(
                entryId = 1L,
                sourceQualityKey = "source-1080",
                subtitleKey = "subs",
                playerQualityMode = PlayerQualityMode.SPECIFIC_HEIGHT,
                playerQualityHeight = 1080,
            ),
        )
        val interactions = createEntryInteractions(listOf(animeEntryInteractionPlugin(dependencies)))

        val anime = entry(EntryType.ANIME, id = 1L)
        val progressSnapshot = interactions.progress.snapshot(anime)
        val preferencesSnapshot = interactions.playbackPreferences.snapshot(anime)

        progressSnapshot shouldBe EntryProgressSnapshot(
            states = listOf(
                EntryProgressStateSnapshot(
                    resourceKey = "/episode/10",
                    sourceChildKey = "/episode/10",
                    locator = EntryProgressLocator(
                        kind = "time",
                        position = 12_000L,
                        extent = 60_000L,
                        progression = 0.2,
                    ),
                    completed = false,
                    locatorUpdatedAt = 123L,
                    completionUpdatedAt = 123L,
                ),
            ),
        )
        preferencesSnapshot shouldBe EntryPlaybackPreferencesSnapshot(
            sourceQualityKey = "source-1080",
            subtitleKey = "subs",
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
    }

    @Test
    fun `anime progress restore maps portable child key and preferences restore independently`() = runTest {
        val progressRepository = FakeEntryProgressRepository(emptyList())
        val preferencesRepository = FakePlaybackPreferencesRepository()
        val progressProcessor = AnimeProgressProcessor(
            entryProgressRepository = progressRepository,
            entryChapterRepository = FakeEntryChapterRepository(listOf(chapter(id = 20L, entryId = 2L))),
        )
        val preferencesProcessor = AnimePlaybackPreferencesProcessor(preferencesRepository)

        progressProcessor.restore(
            entry = entry(EntryType.ANIME, id = 2L),
            snapshot = EntryProgressSnapshot(
                states = listOf(
                    EntryProgressStateSnapshot(
                        resourceKey = "/episode/20",
                        sourceChildKey = "/episode/20",
                        locator = EntryProgressLocator(kind = "time", position = 1_000L, extent = 2_000L),
                        completed = true,
                        locatorUpdatedAt = 50L,
                        completionUpdatedAt = 50L,
                    ),
                ),
            ),
        )
        preferencesProcessor.restore(
            entry(EntryType.ANIME, id = 2L),
            EntryPlaybackPreferencesSnapshot(
                dubKey = "dub",
                streamKey = "stream",
                sourceQualityKey = "quality",
                subtitleKey = "subtitle",
                playerQualityMode = EntryPlaybackQualityMode.SPECIFIC_HEIGHT,
                playerQualityHeight = 720,
                subtitleOffsetX = 0.1,
                subtitleOffsetY = 0.2,
                subtitleTextSize = 1.3,
                subtitleTextColor = 0xFFFFFF,
                subtitleBackgroundColor = 0,
                subtitleBackgroundOpacity = 0.4,
                updatedAt = 60L,
            ),
        )

        progressRepository.upsertedStates.shouldContainExactly(
            playbackState(
                entryId = 2L,
                chapterId = 20L,
                positionMs = 1_000L,
                durationMs = 2_000L,
                completed = true,
                lastWatchedAt = 50L,
            ).copy(locator = EntryProgressLocator(kind = "time", position = 1_000L, extent = 2_000L)),
        )
        preferencesRepository.upsertedPreferences.shouldContainExactly(
            playbackPreferences(
                entryId = 2L,
                dubKey = "dub",
                streamKey = "stream",
                sourceQualityKey = "quality",
                subtitleKey = "subtitle",
                playerQualityMode = PlayerQualityMode.SPECIFIC_HEIGHT,
                playerQualityHeight = 720,
                updatedAt = 60L,
            ),
        )
    }

    @Test
    fun `anime progress copy maps resource state and preferences copy independently`() = runTest {
        val progressRepository = FakeEntryProgressRepository(
            listOf(
                playbackState(entryId = 1L, chapterId = 10L, positionMs = 3_000L, completed = false),
                playbackState(entryId = 1L, chapterId = 11L, positionMs = 4_000L, completed = true),
            ),
        )
        val preferencesRepository = FakePlaybackPreferencesRepository(
            playbackPreferences(entryId = 1L, streamKey = "stream", updatedAt = 70L),
        )
        val progressProcessor = AnimeProgressProcessor(progressRepository, FakeEntryChapterRepository(emptyList()))
        val preferencesProcessor = AnimePlaybackPreferencesProcessor(preferencesRepository)

        progressProcessor.copy(
            sourceEntry = entry(EntryType.ANIME, id = 1L),
            targetEntry = entry(EntryType.ANIME, id = 2L),
            resourceMappings = listOf(
                EntryProgressResourceMapping(
                    sourceResourceKey = "/episode/10",
                    targetResourceKey = "/episode/20",
                    targetChapterId = 20L,
                ),
            ),
        )
        preferencesProcessor.copy(entry(EntryType.ANIME, id = 1L), entry(EntryType.ANIME, id = 2L))

        progressRepository.upsertedStates.shouldContainExactly(
            playbackState(entryId = 2L, chapterId = 20L, positionMs = 3_000L, completed = false),
        )
        preferencesRepository.upsertedPreferences.shouldContainExactly(
            playbackPreferences(entryId = 2L, streamKey = "stream", updatedAt = 70L),
        )
    }

    @Test
    fun `anime processors reject manga entries`() = runTest {
        val dependencies = dependencies()
        val openProcessor = AnimeOpenProcessor()
        val continueProcessor = AnimeContinueProcessor(
            getEntryWithChapters = dependencies.getEntryWithChapters,
            entryProgressRepository = dependencies.entryProgressRepository,
            openProcessor = openProcessor,
        )
        val downloadProcessor = AnimeDownloadProcessor(dependencies)
        val consumptionProcessor = AnimeConsumptionProcessor(
            entryProgressRepository = dependencies.entryProgressRepository,
        )
        val mangaEntry = entry(EntryType.MANGA)

        val openError = assertFailsWith<IllegalArgumentException> {
            openProcessor.open(context, mangaEntry, chapter(), EntryOpenOptions())
        }
        val continueError = assertFailsWith<IllegalArgumentException> {
            continueProcessor.findNext(mangaEntry)
        }
        val downloadError = assertFailsWith<IllegalArgumentException> {
            downloadProcessor.download(mangaEntry, listOf(chapter()), startNow = false)
        }
        val consumptionError = assertFailsWith<IllegalArgumentException> {
            consumptionProcessor.setConsumed(mangaEntry, listOf(chapter()), consumed = true)
        }

        openError.message shouldContain "expected ANIME"
        continueError.message shouldContain "expected ANIME"
        downloadError.message shouldContain "expected ANIME"
        consumptionError.message shouldContain "expected ANIME"
    }

    @Test
    fun `anime continue prefers first in-progress playback state by source order`() = runTest {
        val expected = chapter(id = 2L, read = false, sourceOrder = 1L)
        val dependencies = dependencies(
            chapters = listOf(
                chapter(id = 1L, read = false, sourceOrder = 0L),
                expected,
                chapter(id = 3L, read = false, sourceOrder = 2L),
            ),
            playbackStates = listOf(
                playbackState(chapterId = 3L, positionMs = 20_000L, completed = false),
                playbackState(chapterId = 2L, positionMs = 1L, completed = false),
                playbackState(chapterId = 1L, positionMs = 10_000L, completed = true),
            ),
        )
        val processor = AnimeContinueProcessor(
            getEntryWithChapters = dependencies.getEntryWithChapters,
            entryProgressRepository = dependencies.entryProgressRepository,
            openProcessor = AnimeOpenProcessor(),
        )

        val result = processor.findNext(entry(EntryType.ANIME, id = 1L))

        result shouldBe expected
    }

    @Test
    fun `anime continue falls back to next unread episode by source order`() = runTest {
        val expected = chapter(id = 2L, read = false, sourceOrder = 4L)
        val dependencies = dependencies(
            chapters = listOf(
                chapter(id = 1L, read = true, sourceOrder = 0L),
                expected,
                chapter(id = 3L, read = false, sourceOrder = 2L),
            ),
            playbackStates = listOf(
                playbackState(chapterId = 2L, positionMs = 0L, completed = false),
                playbackState(chapterId = 3L, positionMs = 10_000L, completed = true),
            ),
        )
        val processor = AnimeContinueProcessor(
            getEntryWithChapters = dependencies.getEntryWithChapters,
            entryProgressRepository = dependencies.entryProgressRepository,
            openProcessor = AnimeOpenProcessor(),
        )

        val result = processor.findNext(entry(EntryType.ANIME, id = 1L))

        result shouldBe expected
    }

    @Test
    fun `anime continue returns null when no episode is available`() = runTest {
        val dependencies = dependencies(
            chapters = listOf(
                chapter(id = 1L, read = true, sourceOrder = 0L),
                chapter(id = 2L, read = true, sourceOrder = 1L),
            ),
            playbackStates = listOf(
                playbackState(chapterId = 1L, positionMs = 10_000L, completed = true),
                playbackState(chapterId = 2L, positionMs = 0L, completed = false),
            ),
        )
        val processor = AnimeContinueProcessor(
            getEntryWithChapters = dependencies.getEntryWithChapters,
            entryProgressRepository = dependencies.entryProgressRepository,
            openProcessor = AnimeOpenProcessor(),
        )

        val result = processor.findNext(entry(EntryType.ANIME, id = 1L))

        result.shouldBeNull()
    }

    @Test
    fun `facade continue opens through anime open processor`() = runTest {
        val opened = mutableListOf<Pair<Long, Long>>()
        val dependencies = dependencies(
            chapters = listOf(chapter(id = 22L, entryId = 7L, read = false)),
        )
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    val openProcessor = AnimeOpenProcessor(openEpisode = { _, entry, chapter, _ ->
                        opened += entry.id to chapter.id
                    })
                    registry.registerContinueProcessor(
                        AnimeContinueProcessor(
                            getEntryWithChapters = dependencies.getEntryWithChapters,
                            entryProgressRepository = dependencies.entryProgressRepository,
                            openProcessor = openProcessor,
                        ),
                    )
                },
            ),
        )

        val result = interactions.continueEntry.continueEntry(context, entry(EntryType.ANIME, id = 7L))

        result?.id shouldBe 22L
        opened.shouldContainExactly(7L to 22L)
    }

    @Test
    fun `anime continue opens merged member episode from visible target entry`() = runTest {
        val opened = mutableListOf<Triple<Long, Long, Long>>()
        val targetEntry = entry(EntryType.ANIME, id = 7L, title = "Merged anime")
        val memberEntry = entry(EntryType.ANIME, id = 8L, title = "Member anime")
        val memberEpisode = chapter(id = 82L, entryId = 8L, read = false, sourceOrder = 1L)
        val dependencies = dependencies(
            entries = listOf(targetEntry, memberEntry),
            chapters = listOf(
                chapter(id = 71L, entryId = 7L, read = true, sourceOrder = 0L),
                memberEpisode,
            ),
            merges = listOf(
                EntryMerge(targetId = 7L, entryId = 7L, position = 0L),
                EntryMerge(targetId = 7L, entryId = 8L, position = 1L),
            ),
            playbackStates = listOf(
                playbackState(entryId = 8L, chapterId = 82L, positionMs = 5_000L, completed = false),
            ),
        )
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    val openProcessor = AnimeOpenProcessor(openEpisode = { _, entry, chapter, _ ->
                        opened += Triple(entry.id, chapter.entryId, chapter.id)
                    })
                    registry.registerContinueProcessor(
                        AnimeContinueProcessor(
                            getEntryWithChapters = dependencies.getEntryWithChapters,
                            entryProgressRepository = dependencies.entryProgressRepository,
                            openProcessor = openProcessor,
                        ),
                    )
                },
            ),
        )

        val result = interactions.continueEntry.continueEntry(context, targetEntry)

        result shouldBe memberEpisode
        opened.shouldContainExactly(Triple(7L, 8L, 82L))
    }

    @Test
    fun `anime download state mapping maps real runtime states`() {
        AnimeDownload.State.NOT_DOWNLOADED.toEntryDownloadState() shouldBe EntryDownloadState.NOT_DOWNLOADED
        AnimeDownload.State.QUEUE.toEntryDownloadState() shouldBe EntryDownloadState.QUEUE
        AnimeDownload.State.RESOLVING.toEntryDownloadState() shouldBe EntryDownloadState.DOWNLOADING
        AnimeDownload.State.DOWNLOADING.toEntryDownloadState() shouldBe EntryDownloadState.DOWNLOADING
        AnimeDownload.State.DOWNLOADED.toEntryDownloadState() shouldBe EntryDownloadState.DOWNLOADED
        AnimeDownload.State.ERROR.toEntryDownloadState() shouldBe EntryDownloadState.ERROR
    }

    @Test
    fun `anime consumption marks unwatched and resets playback progress`() = runTest {
        val chapterRepository = FakeEntryChapterRepository(
            listOf(
                chapter(id = 1L, read = true),
                chapter(id = 2L, read = true),
            ),
        )
        val playbackRepository = FakeEntryProgressRepository(
            listOf(
                playbackState(chapterId = 1L, positionMs = 20_000L, completed = true),
                playbackState(chapterId = 2L, positionMs = 10_000L, completed = true),
            ),
        )
        val processor = AnimeConsumptionProcessor(
            entryProgressRepository = playbackRepository,
            now = { 100L },
        )

        processor.setConsumed(
            entry = entry(EntryType.ANIME),
            chapters = listOf(
                chapter(id = 1L, read = true),
                chapter(id = 2L, read = true),
            ),
            consumed = false,
        )

        playbackRepository.upsertedStates.shouldContainExactly(
            playbackState(chapterId = 1L, positionMs = 0L, durationMs = 0L, completed = false, lastWatchedAt = 100L)
                .copy(locator = EntryProgressLocator(kind = "time")),
            playbackState(chapterId = 2L, positionMs = 0L, durationMs = 0L, completed = false, lastWatchedAt = 100L)
                .copy(locator = EntryProgressLocator(kind = "time")),
        )
    }

    @Test
    fun `anime consumption marks watched and completes playback state`() = runTest {
        val chapterRepository = FakeEntryChapterRepository(
            listOf(
                chapter(id = 1L, read = false),
                chapter(id = 2L, read = true),
            ),
        )
        val playbackRepository = FakeEntryProgressRepository(
            listOf(
                playbackState(chapterId = 1L, positionMs = 20_000L, completed = false),
                playbackState(chapterId = 2L, positionMs = 10_000L, completed = false),
            ),
        )
        val processor = AnimeConsumptionProcessor(
            entryProgressRepository = playbackRepository,
            now = { 100L },
        )

        processor.setConsumed(
            entry = entry(EntryType.ANIME),
            chapters = listOf(
                chapter(id = 1L, read = false),
                chapter(id = 2L, read = true),
            ),
            consumed = true,
        )

        playbackRepository.upsertedStates.shouldContainExactly(
            playbackState(chapterId = 1L, positionMs = 20_000L, completed = true)
                .copy(completionUpdatedAt = 100L),
        )
    }

    @Test
    fun `anime download model maps to entry status queue item and group`() {
        val download = AnimeDownload(
            anime = entry(EntryType.ANIME, id = 7L, title = "Entry", sourceId = 2L),
            episode = chapter(id = 9L, name = "Episode 9", dateUpload = 123L, chapterNumber = 9.0),
            preferences = downloadPreferences(entryId = 7L),
        ).apply {
            status = AnimeDownload.State.RESOLVING
            progress = 30
        }
        val sourceManager = mockSourceManager(source(id = 2L, name = "Source"))

        val status = download.toEntryDownloadStatus()
        val item = download.toEntryDownloadQueueItem()
        val groups = listOf(download).toAnimeEntryDownloadQueueGroups(sourceManager)

        status.entryType shouldBe EntryType.ANIME
        status.chapterId shouldBe 9L
        status.state shouldBe EntryDownloadState.DOWNLOADING
        status.progress shouldBe 30
        item.entryId shouldBe 7L
        item.childId shouldBe 9L
        item.title shouldBe "Entry"
        item.subtitle shouldBe "Episode 9"
        item.progress shouldBe 30
        item.progressMax shouldBe 100
        item.progressText shouldBe "Resolving"
        groups.map { it.sourceName }.shouldContainExactly("Source")
    }

    @Test
    fun `anime preview config stays disabled while preview implementation is unsupported`() = runTest {
        val entryInteractionPreferences = EntryInteractionPreferences(InMemoryPreferenceStore())
        entryInteractionPreferences.enableAnimePreview.set(true)
        val interactions = createEntryInteractions(
            listOf(
                animeEntryInteractionPlugin(
                    dependencies(entryInteractionPreferences = entryInteractionPreferences),
                ),
            ),
        )

        interactions.preview.config(entry(EntryType.ANIME)).enabled shouldBe false
    }

    @Test
    fun `anime preview remains registered for a preview capable source`() = runTest {
        val entryInteractionPreferences = EntryInteractionPreferences(InMemoryPreferenceStore())
        entryInteractionPreferences.enableAnimePreview.set(true)
        val previewSource = mockk<EntryPreviewSource>(relaxed = true) {
            every { id } returns 1L
            every { name } returns "Preview source"
        }
        val interactions = createEntryInteractions(
            listOf(
                animeEntryInteractionPlugin(
                    dependencies(
                        entryInteractionPreferences = entryInteractionPreferences,
                        sourceManager = mockSourceManager(previewSource),
                    ),
                ),
            ),
        )

        interactions.preview.isSupported(entry(EntryType.ANIME, sourceId = 1L)) shouldBe true
        interactions.preview.config(entry(EntryType.ANIME, sourceId = 1L)).enabled shouldBe true
    }

    private fun dependencies(
        chapters: List<EntryChapter> = emptyList(),
        entries: List<Entry> = listOf(entry(EntryType.ANIME)),
        merges: List<EntryMerge> = emptyList(),
        playbackStates: List<EntryProgressState> = emptyList(),
        playbackPreferences: PlaybackPreferences? = null,
        episodeDownloaded: Boolean = false,
        entryInteractionPreferences: EntryInteractionPreferences =
            EntryInteractionPreferences(InMemoryPreferenceStore()),
        animeDownloadManager: AnimeDownloadManager? = null,
        downloadPreferencesRepository: DownloadPreferencesRepository = FakeDownloadPreferencesRepository(),
        sourceManager: SourceManager = mockSourceManager(),
    ): AnimeEntryInteractionRuntimeDependencies {
        val entryChapterRepository = FakeEntryChapterRepository(chapters)
        return AnimeEntryInteractionRuntimeDependencies(
            entryChapterRepository = entryChapterRepository,
            getEntryWithChapters = GetEntryWithChapters(
                entryRepository = fakeEntryRepository(entries),
                entryChapterRepository = entryChapterRepository,
                mergedEntryRepository = fakeMergedEntryRepository(merges),
            ),
            entryProgressRepository = FakeEntryProgressRepository(playbackStates),
            playbackPreferencesRepository = FakePlaybackPreferencesRepository(playbackPreferences),
            animeDownloadManager = animeDownloadManager ?: mockAnimeDownloadManager(episodeDownloaded),
            animeDownloadCache = mockAnimeDownloadCache(),
            downloadPreferences = mockk(relaxed = true),
            downloadPreferencesRepository = downloadPreferencesRepository,
            sourceManager = sourceManager,
            entryInteractionPreferences = entryInteractionPreferences,
        )
    }

    private fun mockAnimeDownloadManager(episodeDownloaded: Boolean): AnimeDownloadManager {
        val queueState = MutableStateFlow<List<AnimeDownload>>(emptyList())
        return mockk(relaxed = true) {
            every { this@mockk.queueState } returns queueState
            every { this@mockk.cacheChanges } returns MutableSharedFlow<Unit>()
            every { this@mockk.isRunning } returns MutableStateFlow(false)
            every { this@mockk.statusFlow() } returns emptyFlow()
            every { this@mockk.progressFlow() } returns emptyFlow()
            every { this@mockk.isEpisodeDownloaded(any(), any(), any(), any(), any()) } returns episodeDownloaded
            every { this@mockk.getDownloadCount(any<Entry>()) } returns 0
            every { this@mockk.getTotalDownloadCount() } returns 0
        }
    }

    private fun mockAnimeDownloadCache(): AnimeDownloadCache {
        return mockk(relaxed = true) {
            every { this@mockk.changes } returns MutableSharedFlow<Unit>()
            every { this@mockk.isInitializing } returns MutableStateFlow(false)
        }
    }

    private fun mockSourceManager(source: UnifiedSource = source()): SourceManager {
        return mockk(relaxed = true) {
            every { this@mockk.get(any()) } returns source
            every { this@mockk.getOrStub(any()) } returns source
        }
    }

    private fun fakeEntryRepository(entries: List<Entry>): EntryRepository {
        return mockk(relaxed = true) {
            coEvery { this@mockk.getEntryById(any()) } answers {
                entries.firstOrNull { it.id == firstArg<Long>() }
            }
        }
    }

    private fun fakeMergedEntryRepository(merges: List<EntryMerge>): MergedEntryRepository {
        return mockk(relaxed = true) {
            coEvery { this@mockk.getGroupByEntryId(any()) } answers {
                val entryId = firstArg<Long>()
                val targetId = merges.firstOrNull { it.entryId == entryId }?.targetId ?: return@answers emptyList()
                merges.filter { it.targetId == targetId }
            }
        }
    }

    private fun source(id: Long = 1L, name: String = "Source"): UnifiedSource {
        return mockk {
            every { this@mockk.id } returns id
            every { this@mockk.name } returns name
        }
    }

    private fun entry(
        type: EntryType,
        id: Long = 1L,
        title: String = "Entry",
        sourceId: Long = 1L,
    ): Entry {
        return Entry.create().copy(id = id, title = title, source = sourceId, type = type)
    }

    private fun libraryItem(
        entryId: Long,
        continueEntryId: Long?,
    ): LibraryItem {
        val entry = entry(EntryType.ANIME, id = entryId)
        return LibraryItem(
            entry = entry,
            categories = emptyList(),
            sourceName = "Source",
            sourceLanguage = "en",
            sourceItemOrientation = EntryItemOrientation.VERTICAL,
            displaySourceId = entry.source,
            sourceIds = setOf(entry.source),
            isLocal = false,
            isMerged = false,
            memberEntryIds = emptyList(),
            memberEntries = listOf(entry),
            progress = ProgressState(
                totalCount = 1L,
                consumedCount = 0L,
                hasStarted = false,
                continueMode = ProgressState.ContinueMode.TARGET_AVAILABLE,
            ),
            latestUpload = 0L,
            lastRead = 0L,
            continueEntryId = continueEntryId,
            downloadCount = 0,
        )
    }

    private fun chapter(
        id: Long = 1L,
        entryId: Long = 1L,
        name: String = "Episode",
        read: Boolean = false,
        lastPageRead: Long = 0L,
        sourceOrder: Long = 0L,
        dateUpload: Long = 0L,
        chapterNumber: Double = 0.0,
    ): EntryChapter {
        return EntryChapter.create().copy(
            id = id,
            entryId = entryId,
            url = "/episode/$id",
            name = name,
            read = read,
            lastPageRead = lastPageRead,
            sourceOrder = sourceOrder,
            dateUpload = dateUpload,
            chapterNumber = chapterNumber,
        )
    }

    private fun playbackState(
        entryId: Long = 1L,
        chapterId: Long,
        positionMs: Long,
        completed: Boolean = false,
        durationMs: Long = 60_000L,
        lastWatchedAt: Long = 0L,
    ): EntryProgressState {
        return animeProgressState(
            entryId = entryId,
            chapterId = chapterId,
            resourceKey = "/episode/$chapterId",
            positionMs = positionMs,
            durationMs = durationMs,
            completed = completed,
            locatorUpdatedAt = lastWatchedAt,
            completionUpdatedAt = lastWatchedAt,
        )
    }

    private fun playbackPreferences(
        entryId: Long,
        dubKey: String? = null,
        streamKey: String? = null,
        sourceQualityKey: String? = null,
        subtitleKey: String? = null,
        playerQualityMode: PlayerQualityMode = PlayerQualityMode.AUTO,
        playerQualityHeight: Int? = null,
        subtitleOffsetX: Double? = 0.1,
        subtitleOffsetY: Double? = 0.2,
        subtitleTextSize: Double? = 1.3,
        subtitleTextColor: Int? = 0xFFFFFF,
        subtitleBackgroundColor: Int? = 0,
        subtitleBackgroundOpacity: Double? = 0.4,
        updatedAt: Long = 99L,
    ): PlaybackPreferences {
        return PlaybackPreferences(
            entryId = entryId,
            dubKey = dubKey,
            streamKey = streamKey,
            sourceQualityKey = sourceQualityKey,
            subtitleKey = subtitleKey,
            playerQualityMode = playerQualityMode,
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

    private fun downloadPreferences(entryId: Long): DownloadPreferences {
        return DownloadPreferences(
            entryId = entryId,
            dubKey = null,
            streamKey = null,
            subtitleKey = null,
            qualityMode = VideoDownloadQualityMode.BALANCED,
            updatedAt = 0L,
        )
    }

    private class FakeEntryChapterRepository(
        private val chapters: List<EntryChapter>,
    ) : EntryChapterRepository {
        val updatedChapters = mutableListOf<EntryChapter>()

        override suspend fun getChapterById(id: Long): EntryChapter? = chapters.firstOrNull { it.id == id }

        override fun getChaptersByEntryId(entryId: Long): Flow<List<EntryChapter>> {
            return flowOf(chapters.filter { it.entryId == entryId })
        }

        override fun getChaptersByEntryIds(entryIds: List<Long>): Flow<List<EntryChapter>> {
            return flowOf(chapters.filter { it.entryId in entryIds })
        }

        override suspend fun getChaptersByEntryIdAwait(
            entryId: Long,
            applyScanlatorFilter: Boolean,
        ): List<EntryChapter> {
            return chapters.filter { it.entryId == entryId }
        }

        override suspend fun getRecentRead(offset: Int, limit: Int): List<EntryChapter> = emptyList()

        override suspend fun getBookmarkedChaptersByEntryId(entryId: Long): List<EntryChapter> {
            return chapters.filter { it.entryId == entryId && it.bookmark }
        }

        override suspend fun insert(chapter: EntryChapter): Long = chapter.id

        override suspend fun insertOrUpdate(chapters: List<EntryChapter>): List<EntryChapter> = chapters

        override suspend fun update(chapter: EntryChapter): Boolean = true

        override suspend fun updateAll(chapters: List<EntryChapter>): Boolean {
            updatedChapters += chapters
            return true
        }

        override suspend fun delete(id: Long): Boolean = true

        override suspend fun deleteByEntryId(entryId: Long): Boolean = true

        override suspend fun removeChaptersWithIds(chapterIds: List<Long>) = Unit

        override suspend fun getScanlatorsByEntryId(entryId: Long): List<String> = emptyList()

        override fun getScanlatorsByEntryIdAsFlow(entryId: Long): Flow<List<String>> = flowOf(emptyList())

        override suspend fun getChapterByUrlAndEntryId(url: String, entryId: Long): EntryChapter? {
            return chapters.firstOrNull { it.url == url && it.entryId == entryId }
        }
    }

    private class FakeEntryProgressRepository(
        initialStates: List<EntryProgressState>,
    ) : EntryProgressRepository {
        private val states = initialStates.toMutableList()
        val upsertedStates = mutableListOf<EntryProgressState>()

        override suspend fun get(entryId: Long, contentKey: String, resourceKey: String): EntryProgressState? {
            return states.firstOrNull {
                it.entryId == entryId && it.contentKey == contentKey && it.resourceKey == resourceKey
            }
        }

        override suspend fun getByEntryId(entryId: Long): List<EntryProgressState> =
            states.filter { it.entryId == entryId }

        override fun getByEntryIdAsFlow(entryId: Long): Flow<List<EntryProgressState>> =
            flowOf(states.filter { it.entryId == entryId })

        override fun getByChapterIdAsFlow(chapterId: Long): Flow<List<EntryProgressState>> =
            flowOf(states.filter { it.chapterId == chapterId })

        override suspend fun upsert(state: EntryProgressState) = record(state)

        override suspend fun upsertAndSyncChild(state: EntryProgressState) = record(state)

        override suspend fun merge(state: EntryProgressState): EntryProgressState = state.also(::record)

        override suspend fun mergeAndSyncChild(state: EntryProgressState): EntryProgressState = state.also(::record)

        private fun record(state: EntryProgressState) {
            states.removeAll { it.identity == state.identity }
            states += state
            upsertedStates += state
        }
    }

    private class FakePlaybackPreferencesRepository(
        private val preferences: PlaybackPreferences? = null,
    ) : PlaybackPreferencesRepository {
        val upsertedPreferences = mutableListOf<PlaybackPreferences>()

        override suspend fun getByEntryId(entryId: Long): PlaybackPreferences? {
            return preferences?.takeIf { it.entryId == entryId }
        }

        override fun getByEntryIdAsFlow(entryId: Long): Flow<PlaybackPreferences?> {
            return flowOf(preferences?.takeIf { it.entryId == entryId })
        }

        override suspend fun upsert(preferences: PlaybackPreferences) {
            upsertedPreferences += preferences
        }
    }

    private class FakeDownloadPreferencesRepository(
        private val preferences: DownloadPreferences? = null,
    ) : DownloadPreferencesRepository {
        val upserted = mutableListOf<DownloadPreferences>()

        override suspend fun getByEntryId(entryId: Long): DownloadPreferences? = preferences

        override fun getByEntryIdAsFlow(entryId: Long): Flow<DownloadPreferences?> = flowOf(preferences)

        override suspend fun upsert(preferences: DownloadPreferences) {
            upserted += preferences
        }
    }

    private suspend inline fun <reified T : Throwable> assertFailsWith(
        crossinline block: suspend () -> Unit,
    ): T {
        try {
            block()
        } catch (throwable: Throwable) {
            if (throwable is T) return throwable
            throw throwable
        }
        error("Expected ${T::class.simpleName} to be thrown")
    }
}
