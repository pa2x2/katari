package mihon.entry.interactions

import android.app.PendingIntent
import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.entry.VideoRequest
import eu.kanade.tachiyomi.source.entry.VideoStream
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.i18n.MR

class EntryInteractionRegistryTest {
    private val context = mockk<Context>(relaxed = true)
    private val chapter = EntryChapter.create().copy(id = 10L, name = "Chapter")

    @Test
    fun `empty plugin list fails when open interaction is used`() {
        val interactions = createEntryInteractions(emptyList())

        val exception = assertThrows<IllegalStateException> {
            interactions.open.open(context, entry(EntryType.MANGA), chapter)
        }

        exception.message shouldContain "No open processor registered for EntryType MANGA"
        exception.message shouldContain "Registered types: none"
    }

    @Test
    fun `empty plugin list fails when continue interaction is used`() = runTest {
        val interactions = createEntryInteractions(emptyList())

        val exception = assertFailsWith<IllegalStateException> {
            interactions.continueEntry.findNext(entry(EntryType.ANIME))
        }

        exception.message shouldContain "No continue processor registered for EntryType ANIME"
        exception.message shouldContain "Registered types: none"
    }

    @Test
    fun `empty plugin list fails when download interaction is used`() = runTest {
        val interactions = createEntryInteractions(emptyList())

        val exception = assertFailsWith<IllegalStateException> {
            interactions.download.download(entry(EntryType.MANGA), listOf(chapter))
        }

        exception.message shouldContain "No download processor registered for EntryType MANGA"
        exception.message shouldContain "Registered types: none"
    }

    @Test
    fun `missing download processor exposes unsupported capability with neutral query results`() = runTest {
        val interactions = createEntryInteractions(emptyList())
        val entry = entry(EntryType.BOOK)

        interactions.download.supportsDownloads(EntryType.BOOK) shouldBe false
        interactions.download.supportsDownloadOptions(entry) shouldBe false
        interactions.download.supportsBulkDownload(entry) shouldBe false
        interactions.download.resolveDownloadOptions(context, entry, chapter).shouldBeNull()
        interactions.download.resolveBulkDownloadCandidates(
            entry = entry,
            action = EntryBulkDownloadAction.unread,
        ) shouldBe EntryBulkDownloadCandidateResult.Unsupported
        interactions.download.filterAutoDownloadCandidates(entry, listOf(chapter)) shouldBe emptyList()
        interactions.download.hasDownloads(entry) shouldBe false
        interactions.download.getDownloadCount(entry) shouldBe 0
        interactions.download.isDownloaded(entry, chapter) shouldBe false
        interactions.download.getStatus(
            entryType = EntryType.BOOK,
            chapterId = chapter.id,
            chapterName = chapter.name,
            chapterScanlator = chapter.scanlator,
            chapterUrl = chapter.url,
            entryTitle = entry.title,
            sourceId = entry.source,
        ) shouldBe EntryDownloadStatus(EntryType.BOOK, chapter.id, EntryDownloadState.NOT_DOWNLOADED)
        interactions.download.cancelQueuedDownload(EntryType.BOOK, chapter.id).shouldBeNull()

        interactions.download.delete(entry, listOf(chapter))
        interactions.download.deleteEntryDownloads(entry)
        interactions.download.renameEntry(entry, "Renamed")
    }

    @Test
    fun `empty plugin list fails when consumption interaction is used`() = runTest {
        val interactions = createEntryInteractions(emptyList())

        val exception = assertFailsWith<IllegalStateException> {
            interactions.consumption.setConsumed(entry(EntryType.MANGA), listOf(chapter), consumed = true)
        }

        exception.message shouldContain "No consumption processor registered for EntryType MANGA"
        exception.message shouldContain "Registered types: none"
    }

    @Test
    fun `empty plugin list reports child group filtering unsupported`() {
        val interactions = createEntryInteractions(emptyList())

        interactions.childGroupFilter.supports(entry(EntryType.MANGA)) shouldBe false
        interactions.childGroupFilter.shouldApplyFilter(entry(EntryType.MANGA)) shouldBe false
    }

    @Test
    fun `empty plugin list reports library filtering unsupported`() {
        val interactions = createEntryInteractions(emptyList())

        interactions.libraryFilter.supportsOutsideReleasePeriodFilter(entry(EntryType.MANGA)) shouldBe false
    }

    @Test
    fun `empty plugin list treats playback preferences as empty no-op`() = runTest {
        val interactions = createEntryInteractions(emptyList())
        val manga = entry(EntryType.MANGA)
        val preferences = EntryPlaybackPreferencesSnapshot()

        interactions.playbackPreferences.snapshot(manga) shouldBe null
        interactions.playbackPreferences.restore(manga, preferences)
        interactions.playbackPreferences.copy(manga, manga.copy(id = 2L))
    }

    @Test
    fun `empty plugin list treats progress as empty no-op`() = runTest {
        val interactions = createEntryInteractions(emptyList())
        val manga = entry(EntryType.MANGA)

        interactions.progress.snapshot(manga) shouldBe EntryProgressSnapshot()
        interactions.progress.restore(manga, EntryProgressSnapshot())
        interactions.progress.copy(manga, manga.copy(id = 2L), emptyList())
    }

    @Test
    fun `empty plugin list reports entry capabilities unsupported`() {
        val interactions = createEntryInteractions(emptyList())
        val manga = entry(EntryType.MANGA)

        interactions.capability.supportsMigration(manga) shouldBe false
        interactions.capability.canMigrate(listOf(manga)) shouldBe false
        interactions.capability.migrationEntries(listOf(manga)) shouldBe emptyList()
        interactions.capability.supportsMerge(manga) shouldBe false
        interactions.capability.canMergeSelection(
            listOf(
                EntryMergeCapabilityItem(manga, isMerged = false),
                EntryMergeCapabilityItem(manga.copy(id = 2L), isMerged = false),
            ),
        ) shouldBe false
        interactions.capability.supportsBulkDownload(manga) shouldBe false
    }

    @Test
    fun `empty plugin list reports immersive feed unsupported`() {
        val interactions = createEntryInteractions(emptyList())

        interactions.immersive.isSupported(entry(EntryType.MANGA)) shouldBe false
    }

    @Test
    fun `immersive feed dispatches by entry type`() = runTest {
        val processor = RecordingImmersiveProcessor(EntryType.ANIME)
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { it.registerImmersiveProcessor(processor) },
            ),
        )
        val anime = entry(EntryType.ANIME)
        val source = mockk<UnifiedSource>()

        val handle = interactions.immersive.load(context, anime, chapter, source)
        interactions.immersive.persistProgress(
            handle,
            EntryImmersiveProgress.Playback(positionMs = 12L, durationMs = 34L),
        )

        interactions.immersive.preloadRadius(EntryType.ANIME) shouldBe 2
        processor.loadedEntryIds shouldContainExactly listOf(anime.id)
        processor.persistedProgress shouldContainExactly listOf(12L to 34L)
    }

    @Test
    fun `duplicate open processor registration fails`() {
        val plugin = EntryInteractionPlugin { registry ->
            registry.registerOpenProcessor(RecordingOpenProcessor(EntryType.MANGA))
            registry.registerOpenProcessor(RecordingOpenProcessor(EntryType.MANGA))
        }

        val exception = assertThrows<IllegalStateException> {
            createEntryInteractions(listOf(plugin))
        }

        exception.message shouldContain "Duplicate open processor registered for EntryType MANGA"
    }

    @Test
    fun `duplicate continue processor registration fails`() {
        val plugin = EntryInteractionPlugin { registry ->
            registry.registerContinueProcessor(RecordingContinueProcessor(EntryType.ANIME))
            registry.registerContinueProcessor(RecordingContinueProcessor(EntryType.ANIME))
        }

        val exception = assertThrows<IllegalStateException> {
            createEntryInteractions(listOf(plugin))
        }

        exception.message shouldContain "Duplicate continue processor registered for EntryType ANIME"
    }

    @Test
    fun `duplicate download processor registration fails`() {
        val plugin = EntryInteractionPlugin { registry ->
            registry.registerDownloadProcessor(RecordingDownloadProcessor(EntryType.MANGA))
            registry.registerDownloadProcessor(RecordingDownloadProcessor(EntryType.MANGA))
        }

        val exception = assertThrows<IllegalStateException> {
            createEntryInteractions(listOf(plugin))
        }

        exception.message shouldContain "Duplicate download processor registered for EntryType MANGA"
    }

    @Test
    fun `duplicate capability processor registration fails`() {
        val plugin = EntryInteractionPlugin { registry ->
            registry.registerCapabilityProcessor(RecordingCapabilityProcessor(EntryType.MANGA))
            registry.registerCapabilityProcessor(RecordingCapabilityProcessor(EntryType.MANGA))
        }

        val exception = assertThrows<IllegalStateException> {
            createEntryInteractions(listOf(plugin))
        }

        exception.message shouldContain "Duplicate capability processor registered for EntryType MANGA"
    }

    @Test
    fun `duplicate consumption processor registration fails`() {
        val plugin = EntryInteractionPlugin { registry ->
            registry.registerConsumptionProcessor(RecordingConsumptionProcessor(EntryType.ANIME))
            registry.registerConsumptionProcessor(RecordingConsumptionProcessor(EntryType.ANIME))
        }

        val exception = assertThrows<IllegalStateException> {
            createEntryInteractions(listOf(plugin))
        }

        exception.message shouldContain "Duplicate consumption processor registered for EntryType ANIME"
    }

    @Test
    fun `duplicate playback preferences processor registration fails`() {
        val plugin = EntryInteractionPlugin { registry ->
            registry.registerPlaybackPreferencesProcessor(RecordingPlaybackPreferencesProcessor(EntryType.ANIME))
            registry.registerPlaybackPreferencesProcessor(RecordingPlaybackPreferencesProcessor(EntryType.ANIME))
        }

        val exception = assertThrows<IllegalStateException> {
            createEntryInteractions(listOf(plugin))
        }

        exception.message shouldContain "Duplicate playback preferences processor registered for EntryType ANIME"
    }

    @Test
    fun `duplicate progress processor registration fails`() {
        val plugin = EntryInteractionPlugin { registry ->
            registry.registerProgressProcessor(RecordingEntryProgressProcessor(EntryType.ANIME))
            registry.registerProgressProcessor(RecordingEntryProgressProcessor(EntryType.ANIME))
        }

        val exception = assertThrows<IllegalStateException> {
            createEntryInteractions(listOf(plugin))
        }

        exception.message shouldContain "Duplicate progress processor registered for EntryType ANIME"
    }

    @Test
    fun `duplicate child group filter processor registration fails`() {
        val plugin = EntryInteractionPlugin { registry ->
            registry.registerChildGroupFilterProcessor(RecordingChildGroupFilterProcessor(EntryType.MANGA))
            registry.registerChildGroupFilterProcessor(RecordingChildGroupFilterProcessor(EntryType.MANGA))
        }

        val exception = assertThrows<IllegalStateException> {
            createEntryInteractions(listOf(plugin))
        }

        exception.message shouldContain "Duplicate child group filter processor registered for EntryType MANGA"
    }

    @Test
    fun `duplicate library filter processor registration fails`() {
        val plugin = EntryInteractionPlugin { registry ->
            registry.registerLibraryFilterProcessor(RecordingLibraryFilterProcessor(EntryType.MANGA))
            registry.registerLibraryFilterProcessor(RecordingLibraryFilterProcessor(EntryType.MANGA))
        }

        val exception = assertThrows<IllegalStateException> {
            createEntryInteractions(listOf(plugin))
        }

        exception.message shouldContain "Duplicate library filter processor registered for EntryType MANGA"
    }

    @Test
    fun `open dispatch selects processor by entry type`() {
        val mangaProcessor = RecordingOpenProcessor(EntryType.MANGA)
        val animeProcessor = RecordingOpenProcessor(EntryType.ANIME)
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    registry.registerOpenProcessor(mangaProcessor)
                    registry.registerOpenProcessor(animeProcessor)
                },
            ),
        )

        interactions.open.open(context, entry(EntryType.ANIME, id = 20L), chapter)

        mangaProcessor.openedEntryIds shouldBe emptyList()
        animeProcessor.openedEntryIds shouldBe listOf(20L)
    }

    @Test
    fun `open dispatch passes options`() {
        val processor = RecordingOpenProcessor(EntryType.MANGA)
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    registry.registerOpenProcessor(processor)
                },
            ),
        )
        val options = EntryOpenOptions(pageIndex = 5, newTask = true, clearTop = true)

        interactions.open.open(context, entry(EntryType.MANGA, id = 21L), chapter, options)

        processor.openOptions shouldBe listOf(options)
    }

    @Test
    fun `pending intent dispatch selects processor by entry type`() {
        val mangaProcessor = RecordingOpenProcessor(EntryType.MANGA)
        val animeProcessor = RecordingOpenProcessor(EntryType.ANIME)
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    registry.registerOpenProcessor(mangaProcessor)
                    registry.registerOpenProcessor(animeProcessor)
                },
            ),
        )
        val options = EntryOpenOptions(ownerEntryId = 100L)

        val result = interactions.open.pendingIntent(context, entry(EntryType.ANIME, id = 22L), chapter, options)

        result shouldBe animeProcessor.pendingIntent
        mangaProcessor.pendingIntentEntryIds shouldBe emptyList()
        animeProcessor.pendingIntentEntryIds shouldBe listOf(22L)
        animeProcessor.pendingIntentOptions shouldBe listOf(options)
    }

    @Test
    fun `continue dispatch selects processor by entry type`() = runTest {
        val mangaProcessor = RecordingContinueProcessor(EntryType.MANGA, nextChapter = chapter.copy(id = 11L))
        val animeProcessor = RecordingContinueProcessor(EntryType.ANIME, nextChapter = chapter.copy(id = 12L))
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    registry.registerContinueProcessor(mangaProcessor)
                    registry.registerContinueProcessor(animeProcessor)
                },
            ),
        )

        val result = interactions.continueEntry.continueEntry(context, entry(EntryType.MANGA, id = 30L))

        result?.id shouldBe 11L
        mangaProcessor.findNextEntryIds shouldBe listOf(30L)
        mangaProcessor.openedChapterIds shouldBe listOf(11L)
        animeProcessor.findNextEntryIds shouldBe emptyList()
        animeProcessor.openedChapterIds shouldBe emptyList()
    }

    @Test
    fun `continue dispatch does not open when no next chapter is found`() = runTest {
        val processor = RecordingContinueProcessor(EntryType.MANGA, nextChapter = null)
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    registry.registerContinueProcessor(processor)
                },
            ),
        )

        val result = interactions.continueEntry.continueEntry(context, entry(EntryType.MANGA))

        result.shouldBeNull()
        processor.openedChapterIds shouldBe emptyList()
    }

    @Test
    fun `download dispatch selects processor by entry type`() = runTest {
        val mangaProcessor = RecordingDownloadProcessor(EntryType.MANGA)
        val animeProcessor = RecordingDownloadProcessor(EntryType.ANIME)
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    registry.registerDownloadProcessor(mangaProcessor)
                    registry.registerDownloadProcessor(animeProcessor)
                },
            ),
        )

        interactions.download.download(entry(EntryType.ANIME, id = 40L), listOf(chapter), startNow = true)

        mangaProcessor.downloadedEntryIds shouldBe emptyList()
        animeProcessor.downloadedEntryIds shouldBe listOf(40L)
        animeProcessor.downloadStartNow shouldBe listOf(true)
    }

    @Test
    fun `bulk download candidate dispatch selects processor by entry type`() = runTest {
        val mangaProcessor = RecordingDownloadProcessor(EntryType.MANGA)
        val animeProcessor = RecordingDownloadProcessor(EntryType.ANIME)
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    registry.registerDownloadProcessor(mangaProcessor)
                    registry.registerDownloadProcessor(animeProcessor)
                },
            ),
        )

        val result = interactions.download.resolveBulkDownloadCandidates(
            entry = entry(EntryType.ANIME, id = 41L),
            action = EntryBulkDownloadAction.next(5),
            candidates = listOf(chapter.copy(id = 42L)),
        )

        result shouldBe EntryBulkDownloadCandidateResult.Supported(listOf(chapter.copy(id = 42L)))
        mangaProcessor.bulkDownloadEntryIds shouldBe emptyList()
        animeProcessor.bulkDownloadEntryIds shouldBe listOf(41L)
        animeProcessor.bulkDownloadActions shouldBe listOf(EntryBulkDownloadAction.next(5))
    }

    @Test
    fun `capability queries preserve migration merge and bulk download policy`() {
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    registry.registerCapabilityProcessor(
                        RecordingCapabilityProcessor(
                            type = EntryType.MANGA,
                            migrationSupported = true,
                            mergeSupported = true,
                        ),
                    )
                    registry.registerCapabilityProcessor(
                        RecordingCapabilityProcessor(
                            type = EntryType.ANIME,
                            migrationSupported = false,
                            mergeSupported = true,
                        ),
                    )
                    registry.registerDownloadProcessor(RecordingDownloadProcessor(EntryType.MANGA))
                    registry.registerDownloadProcessor(
                        RecordingDownloadProcessor(EntryType.ANIME, bulkDownloadSupported = false),
                    )
                },
            ),
        )
        val manga = entry(EntryType.MANGA, id = 1L)
        val otherManga = entry(EntryType.MANGA, id = 2L)
        val anime = entry(EntryType.ANIME, id = 3L)

        interactions.capability.supportsMigration(manga) shouldBe true
        interactions.capability.supportsMigration(anime) shouldBe false
        interactions.capability.canMigrate(listOf(manga, otherManga)) shouldBe true
        interactions.capability.canMigrate(listOf(manga, anime)) shouldBe false
        interactions.capability.migrationEntries(listOf(manga, anime)) shouldContainExactly listOf(manga)

        interactions.capability.canMergeSelection(
            listOf(
                EntryMergeCapabilityItem(manga, isMerged = false),
                EntryMergeCapabilityItem(otherManga, isMerged = true),
            ),
        ) shouldBe true
        interactions.capability.canMergeSelection(
            listOf(
                EntryMergeCapabilityItem(manga, isMerged = false),
                EntryMergeCapabilityItem(anime, isMerged = false),
            ),
        ) shouldBe false
        interactions.capability.canMergeSelection(
            listOf(
                EntryMergeCapabilityItem(manga, isMerged = true),
                EntryMergeCapabilityItem(otherManga, isMerged = true),
            ),
        ) shouldBe false

        interactions.capability.supportsBulkDownload(manga) shouldBe true
        interactions.capability.supportsBulkDownload(anime) shouldBe false
    }

    @Test
    fun `consumption dispatch selects processor by entry type`() = runTest {
        val mangaProcessor = RecordingConsumptionProcessor(EntryType.MANGA)
        val animeProcessor = RecordingConsumptionProcessor(EntryType.ANIME)
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    registry.registerConsumptionProcessor(mangaProcessor)
                    registry.registerConsumptionProcessor(animeProcessor)
                },
            ),
        )

        interactions.consumption.setConsumed(entry(EntryType.ANIME, id = 50L), listOf(chapter), consumed = true)

        mangaProcessor.consumedEntryIds shouldBe emptyList()
        animeProcessor.consumedEntryIds shouldBe listOf(50L)
        animeProcessor.consumedValues shouldBe listOf(true)
    }

    @Test
    fun `bookmark dispatch skips unsupported processors`() = runTest {
        val processor = RecordingConsumptionProcessor(EntryType.ANIME, supportsBookmark = false)
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    registry.registerConsumptionProcessor(processor)
                },
            ),
        )

        interactions.consumption.setBookmarked(entry(EntryType.ANIME, id = 51L), listOf(chapter), bookmarked = true)

        processor.bookmarkedEntryIds shouldBe emptyList()
    }

    @Test
    fun `consumption capability dispatch selects processor by entry type`() {
        val mangaProcessor = RecordingConsumptionProcessor(EntryType.MANGA, resetsPartialProgress = true)
        val animeProcessor = RecordingConsumptionProcessor(EntryType.ANIME, supportsBookmark = false)
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    registry.registerConsumptionProcessor(mangaProcessor)
                    registry.registerConsumptionProcessor(animeProcessor)
                },
            ),
        )

        val unreadWithProgress = EntryConsumptionStatus(
            consumed = false,
            bookmarked = false,
            hasPartialProgress = true,
        )
        val bookmarked = unreadWithProgress.copy(bookmarked = true)

        interactions.consumption.canSetConsumed(
            EntryType.MANGA,
            unreadWithProgress,
            consumed = false,
        ) shouldBe true
        interactions.consumption.canSetConsumed(
            EntryType.ANIME,
            unreadWithProgress,
            consumed = false,
        ) shouldBe false
        interactions.consumption.canSetBookmarked(
            EntryType.MANGA,
            bookmarked,
            bookmarked = false,
        ) shouldBe true
        interactions.consumption.canSetBookmarked(
            EntryType.ANIME,
            bookmarked,
            bookmarked = false,
        ) shouldBe false
    }

    @Test
    fun `playback preferences dispatch selects processor by entry type`() = runTest {
        val mangaProcessor = RecordingPlaybackPreferencesProcessor(EntryType.MANGA)
        val animeProcessor = RecordingPlaybackPreferencesProcessor(EntryType.ANIME)
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    registry.registerPlaybackPreferencesProcessor(mangaProcessor)
                    registry.registerPlaybackPreferencesProcessor(animeProcessor)
                },
            ),
        )
        val anime = entry(EntryType.ANIME, id = 70L)
        val target = anime.copy(id = 71L)

        val snapshot = interactions.playbackPreferences.snapshot(anime)!!
        interactions.playbackPreferences.restore(anime, snapshot)
        interactions.playbackPreferences.copy(anime, target)

        snapshot shouldBe animeProcessor.snapshot
        mangaProcessor.snapshotEntryIds shouldBe emptyList()
        animeProcessor.snapshotEntryIds shouldBe listOf(70L)
        animeProcessor.restoredEntryIds shouldBe listOf(70L)
        animeProcessor.copyRequests shouldBe listOf(70L to 71L)
    }

    @Test
    fun `progress dispatch selects processor by entry type`() = runTest {
        val mangaProcessor = RecordingEntryProgressProcessor(EntryType.MANGA)
        val animeProcessor = RecordingEntryProgressProcessor(EntryType.ANIME)
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    registry.registerProgressProcessor(mangaProcessor)
                    registry.registerProgressProcessor(animeProcessor)
                },
            ),
        )
        val anime = entry(EntryType.ANIME, id = 72L)
        val target = anime.copy(id = 73L)
        val mappings = listOf(
            EntryProgressResourceMapping(
                sourceResourceKey = "/source",
                targetResourceKey = "/target",
                targetChapterId = 74L,
            ),
        )

        val snapshot = interactions.progress.snapshot(anime)
        interactions.progress.restore(anime, snapshot)
        interactions.progress.copy(anime, target, mappings)

        snapshot shouldBe animeProcessor.snapshot
        mangaProcessor.snapshotEntryIds shouldBe emptyList()
        animeProcessor.snapshotEntryIds shouldBe listOf(72L)
        animeProcessor.restoredEntryIds shouldBe listOf(72L)
        animeProcessor.copyRequests shouldBe listOf(72L to 73L)
        animeProcessor.copyMappings shouldBe listOf(mappings)
    }

    @Test
    fun `default child list processor returns empty progress labels`() = runTest {
        val processor = RecordingChildListProcessor(EntryType.MANGA)
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    registry.registerChildListProcessor(processor)
                },
            ),
        )

        val labels = interactions.childList.progressLabels(
            EntryChildProgressRequest(
                entry = entry(EntryType.MANGA, id = 80L),
                chapters = listOf(chapter.copy(id = 81L)),
            ),
        ).first()

        labels shouldBe emptyMap()
    }

    @Test
    fun `child progress labels dispatch selects processor by entry type`() = runTest {
        val mangaProcessor = RecordingProgressChildListProcessor(
            type = EntryType.MANGA,
            labels = mapOf(
                81L to EntryChildProgressLabel(
                    resource = MR.strings.chapter_progress,
                    args = listOf(2L),
                ),
            ),
        )
        val animeProcessor = RecordingProgressChildListProcessor(
            type = EntryType.ANIME,
            labels = mapOf(82L to EntryChildProgressLabel(MR.strings.episode_progress_position, listOf("1:00"))),
        )
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    registry.registerChildListProcessor(mangaProcessor)
                    registry.registerChildListProcessor(animeProcessor)
                },
            ),
        )

        val labels = interactions.childList.progressLabels(
            EntryChildProgressRequest(
                entry = entry(EntryType.ANIME, id = 80L),
                chapters = listOf(chapter.copy(id = 82L)),
                memberIds = listOf(80L),
            ),
        ).first()

        labels shouldBe mapOf(82L to EntryChildProgressLabel(MR.strings.episode_progress_position, listOf("1:00")))
        mangaProcessor.progressRequests shouldBe emptyList()
        animeProcessor.progressRequests.map { it.entry.id } shouldBe listOf(80L)
    }

    @Test
    fun `child group filter dispatch selects processor by entry type`() = runTest {
        val mangaProcessor = RecordingChildGroupFilterProcessor(EntryType.MANGA)
        val animeProcessor = RecordingChildGroupFilterProcessor(EntryType.ANIME, supported = false)
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    registry.registerChildGroupFilterProcessor(mangaProcessor)
                    registry.registerChildGroupFilterProcessor(animeProcessor)
                },
            ),
        )

        val groups = interactions.childGroupFilter.availableGroups(entry(EntryType.MANGA, id = 60L), listOf(60L, 61L))
        interactions.childGroupFilter.setExcludedGroups(entry(EntryType.MANGA, id = 60L), listOf(60L), setOf("A"))

        groups shouldBe setOf("MANGA:60", "MANGA:61")
        mangaProcessor.availableRequests shouldBe listOf(listOf(60L, 61L))
        mangaProcessor.excludedRequests shouldBe listOf(listOf(60L) to setOf("A"))
        animeProcessor.availableRequests shouldBe emptyList()
        animeProcessor.excludedRequests shouldBe emptyList()
    }

    @Test
    fun `unsupported child group filter processor is no-op`() = runTest {
        val processor = RecordingChildGroupFilterProcessor(EntryType.ANIME, supported = false)
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    registry.registerChildGroupFilterProcessor(processor)
                },
            ),
        )
        val anime = entry(EntryType.ANIME, id = 61L)

        interactions.childGroupFilter.supports(anime) shouldBe false
        interactions.childGroupFilter.shouldApplyFilter(anime) shouldBe false
        interactions.childGroupFilter.availableGroups(anime, listOf(61L)) shouldBe emptySet()
        interactions.childGroupFilter.setExcludedGroups(anime, listOf(61L), setOf("A"))

        processor.excludedRequests shouldBe emptyList()
    }

    @Test
    fun `queue dispatch selects processor by entry type`() = runTest {
        val mangaProcessor = RecordingDownloadProcessor(EntryType.MANGA)
        val animeProcessor = RecordingDownloadProcessor(EntryType.ANIME)
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    registry.registerDownloadProcessor(mangaProcessor)
                    registry.registerDownloadProcessor(animeProcessor)
                },
            ),
        )

        interactions.download.queue(entry(EntryType.MANGA, id = 41L), listOf(chapter), autoStart = false)

        mangaProcessor.queuedEntryIds shouldBe listOf(41L)
        mangaProcessor.queueAutoStart shouldBe listOf(false)
        animeProcessor.queuedEntryIds shouldBe emptyList()
    }

    @Test
    fun `download queue operations dispatch by queue item type`() {
        val mangaProcessor = RecordingDownloadProcessor(EntryType.MANGA)
        val animeProcessor = RecordingDownloadProcessor(EntryType.ANIME)
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    registry.registerDownloadProcessor(mangaProcessor)
                    registry.registerDownloadProcessor(animeProcessor)
                },
            ),
        )
        val mangaItem = queueItem(EntryType.MANGA, childId = 1L)
        val animeItem = queueItem(EntryType.ANIME, childId = 2L)

        interactions.download.reorderQueue(listOf(animeItem, mangaItem))
        interactions.download.cancelQueuedDownloads(listOf(mangaItem, animeItem))

        mangaProcessor.reorderedChildIds shouldBe listOf(listOf(1L))
        animeProcessor.reorderedChildIds shouldBe listOf(listOf(2L))
        mangaProcessor.cancelledChildIds shouldBe listOf(listOf(1L))
        animeProcessor.cancelledChildIds shouldBe listOf(listOf(2L))
    }

    @Test
    fun `source rename delegates to all download processors`() {
        val mangaProcessor = RecordingDownloadProcessor(EntryType.MANGA)
        val animeProcessor = RecordingDownloadProcessor(EntryType.ANIME)
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    registry.registerDownloadProcessor(mangaProcessor)
                    registry.registerDownloadProcessor(animeProcessor)
                },
            ),
        )
        val oldSource = source()
        val newSource = source()

        interactions.download.renameSource(oldSource, newSource)

        mangaProcessor.renamedSources shouldBe listOf(oldSource to newSource)
        animeProcessor.renamedSources shouldBe listOf(oldSource to newSource)
    }

    @Test
    fun `preview unsupported returns false`() {
        val interactions = createEntryInteractions(emptyList())

        interactions.preview.isSupported(entry(EntryType.MANGA)) shouldBe false
    }

    @Test
    fun `preview registration dispatches by processor type and evaluates the real entry`() {
        val processor = RecordingPreviewProcessor(EntryType.ANIME, supportedSourceId = 42L)
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    registry.registerPreviewProcessor(processor)
                },
            ),
        )
        val entry = entry(EntryType.ANIME).copy(source = 42L)

        interactions.preview.isSupported(entry) shouldBe true
        processor.requestedSourceIds shouldBe listOf(42L)
    }

    @Test
    fun `mismatched open processor input type fails`() {
        val processor = MutableTypeOpenProcessor(initialType = EntryType.MANGA)
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    registry.registerOpenProcessor(processor)
                },
            ),
        )
        processor.currentType = EntryType.ANIME

        val exception = assertThrows<IllegalArgumentException> {
            interactions.open.open(context, entry(EntryType.MANGA), chapter)
        }

        exception.message shouldContain "Mismatched open processor for EntryType MANGA"
        exception.message shouldContain "processor type was ANIME"
    }

    @Test
    fun `mismatched continue processor input type fails`() = runTest {
        val processor = MutableTypeContinueProcessor(initialType = EntryType.ANIME)
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    registry.registerContinueProcessor(processor)
                },
            ),
        )
        processor.currentType = EntryType.MANGA

        val exception = assertFailsWith<IllegalArgumentException> {
            interactions.continueEntry.findNext(entry(EntryType.ANIME))
        }

        exception.message shouldContain "Mismatched continue processor for EntryType ANIME"
        exception.message shouldContain "processor type was MANGA"
    }

    @Test
    fun `mismatched download processor input type fails`() = runTest {
        val processor = MutableTypeDownloadProcessor(initialType = EntryType.MANGA)
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    registry.registerDownloadProcessor(processor)
                },
            ),
        )
        processor.currentType = EntryType.ANIME

        val exception = assertFailsWith<IllegalArgumentException> {
            interactions.download.hasDownloads(entry(EntryType.MANGA))
        }

        exception.message shouldContain "Mismatched download processor for EntryType MANGA"
        exception.message shouldContain "processor type was ANIME"
    }

    @Test
    fun `mismatched consumption processor input type fails`() = runTest {
        val processor = MutableTypeConsumptionProcessor(initialType = EntryType.ANIME)
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    registry.registerConsumptionProcessor(processor)
                },
            ),
        )
        processor.currentType = EntryType.MANGA

        val exception = assertFailsWith<IllegalArgumentException> {
            interactions.consumption.setConsumed(entry(EntryType.ANIME), listOf(chapter), consumed = true)
        }

        exception.message shouldContain "Mismatched consumption processor for EntryType ANIME"
        exception.message shouldContain "processor type was MANGA"
    }

    @Test
    fun `mismatched child group filter processor input type fails`() = runTest {
        val processor = MutableTypeChildGroupFilterProcessor(initialType = EntryType.MANGA)
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    registry.registerChildGroupFilterProcessor(processor)
                },
            ),
        )
        processor.currentType = EntryType.ANIME

        val exception = assertFailsWith<IllegalArgumentException> {
            interactions.childGroupFilter.availableGroups(entry(EntryType.MANGA), listOf(1L))
        }

        exception.message shouldContain "Mismatched child group filter processor for EntryType MANGA"
        exception.message shouldContain "processor type was ANIME"
    }

    @Test
    fun `download type-specific methods select processor by requested type`() {
        val mangaProcessor = RecordingDownloadProcessor(EntryType.MANGA)
        val animeProcessor = RecordingDownloadProcessor(EntryType.ANIME)
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    registry.registerDownloadProcessor(mangaProcessor)
                    registry.registerDownloadProcessor(animeProcessor)
                },
            ),
        )

        val status = interactions.download.getStatus(
            entryType = EntryType.ANIME,
            chapterId = 55L,
            chapterName = "Episode",
            chapterScanlator = null,
            chapterUrl = "/episode",
            entryTitle = "Title",
            sourceId = 1L,
        )
        interactions.download.cancelQueuedDownload(EntryType.ANIME, 55L)

        status.entryType shouldBe EntryType.ANIME
        animeProcessor.statusRequests.shouldContainExactly(55L)
        animeProcessor.cancelledSingleIds.shouldContainExactly(55L)
        mangaProcessor.statusRequests shouldBe emptyList()
        mangaProcessor.cancelledSingleIds shouldBe emptyList()
    }

    @Test
    fun `library filter dispatch selects processor by entry type`() {
        val mangaProcessor = RecordingLibraryFilterProcessor(EntryType.MANGA, supported = true)
        val animeProcessor = RecordingLibraryFilterProcessor(EntryType.ANIME, supported = false)
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    registry.registerLibraryFilterProcessor(mangaProcessor)
                    registry.registerLibraryFilterProcessor(animeProcessor)
                },
            ),
        )

        interactions.libraryFilter.supportsOutsideReleasePeriodFilter(entry(EntryType.MANGA, id = 10L)) shouldBe true
        interactions.libraryFilter.supportsOutsideReleasePeriodFilter(entry(EntryType.ANIME, id = 20L)) shouldBe false
        mangaProcessor.requestedEntryIds shouldBe listOf(10L)
        animeProcessor.requestedEntryIds shouldBe listOf(20L)
    }

    private fun entry(type: EntryType, id: Long = 1L): Entry {
        return Entry.create().copy(id = id, type = type)
    }

    private fun queueItem(type: EntryType, childId: Long): EntryDownloadQueueItem {
        return EntryDownloadQueueItem(
            entryType = type,
            entryId = 1L,
            childId = childId,
            title = "Title",
            subtitle = "Subtitle",
            dateUpload = 0L,
            chapterNumber = 1.0,
            progress = 0,
            progressMax = 100,
            progressText = "",
        )
    }

    private fun source(): UnifiedSource = mockk(relaxed = true)

    private class RecordingPreviewProcessor(
        override val type: EntryType,
        private val supportedSourceId: Long,
    ) : EntryPreviewProcessor {
        val requestedSourceIds = mutableListOf<Long>()

        override fun isSupported(entry: Entry): Boolean {
            requestedSourceIds += entry.source
            return entry.type == type && entry.source == supportedSourceId
        }

        override fun requiresChapter(entry: Entry): Boolean = true

        override fun config(entry: Entry): EntryPreviewConfig = EntryPreviewConfig.Disabled

        override fun configChanges(entry: Entry): Flow<EntryPreviewConfig> = flowOf(config(entry))

        override suspend fun loadPreview(
            context: Context,
            entry: Entry,
            chapter: EntryChapter?,
            source: UnifiedSource,
            pageCount: Int,
        ): EntryPreviewHandle = error("Not needed for registry dispatch test")

        override suspend fun loadPage(handle: EntryPreviewHandle, pageIndex: Int) = Unit

        override fun release(handle: EntryPreviewHandle) = Unit
    }

    private class RecordingOpenProcessor(
        override val type: EntryType,
    ) : EntryOpenProcessor {
        val openedEntryIds = mutableListOf<Long>()
        val openOptions = mutableListOf<EntryOpenOptions>()
        val pendingIntent: PendingIntent = mockk(relaxed = true)
        val pendingIntentEntryIds = mutableListOf<Long>()
        val pendingIntentOptions = mutableListOf<EntryOpenOptions>()

        override fun open(context: Context, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions) {
            openedEntryIds += entry.id
            openOptions += options
        }

        override fun pendingIntent(
            context: Context,
            entry: Entry,
            chapter: EntryChapter,
            options: EntryOpenOptions,
        ): PendingIntent {
            pendingIntentEntryIds += entry.id
            pendingIntentOptions += options
            return pendingIntent
        }
    }

    private class RecordingContinueProcessor(
        override val type: EntryType,
        private val nextChapter: EntryChapter? = EntryChapter.create(),
    ) : EntryContinueProcessor {
        val findNextEntryIds = mutableListOf<Long>()
        val openedChapterIds = mutableListOf<Long>()

        override suspend fun findNext(entry: Entry): EntryChapter? {
            findNextEntryIds += entry.id
            return nextChapter
        }

        override fun open(context: Context, entry: Entry, chapter: EntryChapter) {
            openedChapterIds += chapter.id
        }
    }

    private open class RecordingDownloadProcessor(
        open override val type: EntryType,
        private val bulkDownloadSupported: Boolean = true,
    ) : EntryDownloadProcessor {
        override val changes: Flow<Unit> = emptyFlow()
        override val isInitializing: Flow<Boolean> = flowOf(false)
        override val isRunning: Flow<Boolean> = flowOf(false)
        override val queueState: Flow<List<EntryDownloadQueueGroup>> = flowOf(emptyList())

        val downloadedEntryIds = mutableListOf<Long>()
        val downloadStartNow = mutableListOf<Boolean>()
        val queuedEntryIds = mutableListOf<Long>()
        val queueAutoStart = mutableListOf<Boolean>()
        val bulkDownloadEntryIds = mutableListOf<Long>()
        val bulkDownloadActions = mutableListOf<EntryBulkDownloadAction>()
        val reorderedChildIds = mutableListOf<List<Long>>()
        val cancelledChildIds = mutableListOf<List<Long>>()
        val statusRequests = mutableListOf<Long>()
        val cancelledSingleIds = mutableListOf<Long>()
        val renamedSources = mutableListOf<Pair<UnifiedSource, UnifiedSource>>()

        override fun updates(): Flow<EntryDownloadStatus> = emptyFlow()

        override fun queueStatusUpdates(): Flow<EntryDownloadQueueItem> = emptyFlow()

        override fun queueProgressUpdates(): Flow<EntryDownloadQueueItem> = emptyFlow()

        override fun startDownloads() = Unit

        override fun pauseDownloads() = Unit

        override fun clearQueue() = Unit

        override fun invalidateCache() = Unit

        override fun renameSource(oldSource: UnifiedSource, newSource: UnifiedSource) {
            renamedSources += oldSource to newSource
        }

        override fun reorderQueue(items: List<EntryDownloadQueueItem>) {
            reorderedChildIds += items.map { it.childId }
        }

        override fun reorderSeries(entryId: Long, moveToTop: Boolean) = Unit

        override fun cancelQueuedDownloads(items: List<EntryDownloadQueueItem>) {
            cancelledChildIds += items.map { it.childId }
        }

        override suspend fun queue(entry: Entry, chapters: List<EntryChapter>, autoStart: Boolean) {
            queuedEntryIds += entry.id
            queueAutoStart += autoStart
        }

        override suspend fun download(entry: Entry, chapters: List<EntryChapter>, startNow: Boolean) {
            downloadedEntryIds += entry.id
            downloadStartNow += startNow
        }

        override fun supportsBulkDownload(entry: Entry): Boolean = bulkDownloadSupported

        override suspend fun resolveBulkDownloadCandidates(
            entry: Entry,
            action: EntryBulkDownloadAction,
            candidates: List<EntryChapter>?,
            memberEntryIds: List<Long>,
        ): EntryBulkDownloadCandidateResult {
            bulkDownloadEntryIds += entry.id
            bulkDownloadActions += action
            return EntryBulkDownloadCandidateResult.Supported(
                candidates ?: listOf(EntryChapter.create().copy(id = 90L)),
            )
        }

        override suspend fun filterAutoDownloadCandidates(
            entry: Entry,
            chapters: List<EntryChapter>,
        ): List<EntryChapter> {
            return chapters
        }

        override suspend fun delete(entry: Entry, chapters: List<EntryChapter>) = Unit

        override suspend fun deleteEntryDownloads(entry: Entry) = Unit

        override fun hasDownloads(entry: Entry): Boolean = false

        override fun getDownloadCount(entry: Entry): Int = 0

        override fun getTotalDownloadCount(): Int = 0

        override fun isDownloaded(entry: Entry, chapter: EntryChapter, skipCache: Boolean): Boolean = false

        override fun getStatus(
            chapterId: Long,
            chapterName: String,
            chapterScanlator: String?,
            chapterUrl: String,
            entryTitle: String,
            sourceId: Long,
        ): EntryDownloadStatus {
            statusRequests += chapterId
            return EntryDownloadStatus(type, chapterId, EntryDownloadState.NOT_DOWNLOADED)
        }

        override fun cancelQueuedDownload(chapterId: Long): EntryDownloadStatus? {
            cancelledSingleIds += chapterId
            return EntryDownloadStatus(type, chapterId, EntryDownloadState.NOT_DOWNLOADED)
        }
    }

    private class RecordingCapabilityProcessor(
        override val type: EntryType,
        private val migrationSupported: Boolean = false,
        private val mergeSupported: Boolean = false,
    ) : EntryCapabilityProcessor {
        override fun supportsMigration(entry: Entry): Boolean = migrationSupported

        override fun supportsMerge(entry: Entry): Boolean = mergeSupported
    }

    private open class RecordingConsumptionProcessor(
        open override val type: EntryType,
        override val supportsBookmark: Boolean = true,
        private val resetsPartialProgress: Boolean = false,
    ) : EntryConsumptionProcessor {
        val consumedEntryIds = mutableListOf<Long>()
        val consumedValues = mutableListOf<Boolean>()
        val bookmarkedEntryIds = mutableListOf<Long>()

        override fun canSetConsumed(status: EntryConsumptionStatus, consumed: Boolean): Boolean {
            return when (consumed) {
                true -> !status.consumed
                false -> status.consumed || (resetsPartialProgress && status.hasPartialProgress)
            }
        }

        override suspend fun setConsumed(entry: Entry, chapters: List<EntryChapter>, consumed: Boolean) {
            consumedEntryIds += entry.id
            consumedValues += consumed
        }

        override suspend fun setBookmarked(entry: Entry, chapters: List<EntryChapter>, bookmarked: Boolean) {
            bookmarkedEntryIds += entry.id
        }
    }

    private class RecordingPlaybackPreferencesProcessor(
        override val type: EntryType,
    ) : EntryPlaybackPreferencesProcessor {
        val snapshot = EntryPlaybackPreferencesSnapshot(
            streamKey = "stream",
            playerQualityMode = EntryPlaybackQualityMode.SPECIFIC_HEIGHT,
            playerQualityHeight = 1080,
            updatedAt = 5L,
        )
        val snapshotEntryIds = mutableListOf<Long>()
        val restoredEntryIds = mutableListOf<Long>()
        val copyRequests = mutableListOf<Pair<Long, Long>>()

        override suspend fun snapshot(entry: Entry): EntryPlaybackPreferencesSnapshot {
            snapshotEntryIds += entry.id
            return snapshot
        }

        override suspend fun restore(entry: Entry, snapshot: EntryPlaybackPreferencesSnapshot) {
            restoredEntryIds += entry.id
        }

        override suspend fun copy(sourceEntry: Entry, targetEntry: Entry) {
            copyRequests += sourceEntry.id to targetEntry.id
        }
    }

    private class RecordingEntryProgressProcessor(
        override val type: EntryType,
    ) : EntryProgressProcessor {
        val snapshot = EntryProgressSnapshot(
            states = listOf(
                EntryProgressStateSnapshot(
                    resourceKey = "/resource",
                    sourceChildKey = "/chapter",
                    locator = EntryProgressLocator(kind = "time", position = 2L, extent = 3L),
                    locatorUpdatedAt = 4L,
                ),
            ),
        )
        val snapshotEntryIds = mutableListOf<Long>()
        val restoredEntryIds = mutableListOf<Long>()
        val copyRequests = mutableListOf<Pair<Long, Long>>()
        val copyMappings = mutableListOf<List<EntryProgressResourceMapping>>()

        override suspend fun snapshot(entry: Entry): EntryProgressSnapshot {
            snapshotEntryIds += entry.id
            return snapshot
        }

        override suspend fun restore(entry: Entry, snapshot: EntryProgressSnapshot) {
            restoredEntryIds += entry.id
        }

        override suspend fun copy(
            sourceEntry: Entry,
            targetEntry: Entry,
            resourceMappings: List<EntryProgressResourceMapping>,
        ) {
            copyRequests += sourceEntry.id to targetEntry.id
            copyMappings += resourceMappings
        }
    }

    private class RecordingImmersiveProcessor(
        override val type: EntryType,
    ) : EntryImmersiveProcessor {
        val loadedEntryIds = mutableListOf<Long>()
        val persistedProgress = mutableListOf<Pair<Long, Long>>()

        override fun isSupported(entry: Entry): Boolean = entry.type == type

        override fun preloadRadius(entryType: EntryType): Int = 2

        override suspend fun load(
            context: Context,
            entry: Entry,
            chapter: EntryChapter,
            source: UnifiedSource,
        ): EntryImmersiveHandle {
            loadedEntryIds += entry.id
            return EntryImmersiveHandle.Playback(
                entryType = type,
                chapterId = chapter.id,
                stream = VideoStream(VideoRequest("https://example.invalid/video")),
                subtitles = emptyList(),
                resumePositionMs = 0L,
            )
        }

        override fun renderer(handle: EntryImmersiveHandle): EntryImmersiveRenderer = mockk(relaxed = true)

        override suspend fun persistProgress(
            handle: EntryImmersiveHandle,
            progress: EntryImmersiveProgress,
        ) {
            val playback = progress as EntryImmersiveProgress.Playback
            persistedProgress += playback.positionMs to playback.durationMs
        }

        override fun release(handle: EntryImmersiveHandle) = Unit
    }

    private open class RecordingChildListProcessor(
        override val type: EntryType,
    ) : EntryChildListProcessor {
        override fun sortedForReading(
            entry: Entry,
            chapters: List<EntryChapter>,
            memberIds: List<Long>,
        ): List<EntryChapter> {
            return chapters
        }

        override fun sortedForDisplay(
            entry: Entry,
            chapters: List<EntryChapter>,
            memberIds: List<Long>,
        ): List<EntryChapter> {
            return chapters
        }

        override fun buildDisplayList(request: EntryChildListRequest): List<EntryChildListRow> {
            return request.chapters.map(EntryChildListRow::Child)
        }
    }

    private class RecordingProgressChildListProcessor(
        type: EntryType,
        private val labels: Map<Long, EntryChildProgressLabel>,
    ) : RecordingChildListProcessor(type) {
        val progressRequests = mutableListOf<EntryChildProgressRequest>()

        override fun progressLabels(
            request: EntryChildProgressRequest,
        ): Flow<Map<Long, EntryChildProgressLabel>> {
            progressRequests += request
            return flowOf(labels)
        }
    }

    private open class RecordingChildGroupFilterProcessor(
        open override val type: EntryType,
        private val supported: Boolean = true,
    ) : EntryChildGroupFilterProcessor {
        val availableRequests = mutableListOf<List<Long>>()
        val excludedRequests = mutableListOf<Pair<List<Long>, Set<String>>>()

        override fun supports(entry: Entry): Boolean = supported

        override fun shouldApplyFilter(entry: Entry): Boolean = supported

        override fun availableGroupsChanged(entryId: Long): Flow<Unit> = emptyFlow()

        override suspend fun availableGroups(entry: Entry, memberIds: Collection<Long>): Set<String> {
            if (!supported) return emptySet()
            availableRequests += memberIds.toList()
            return memberIds.map { "${type.name}:$it" }.toSet()
        }

        override fun excludedGroupsChanged(entryId: Long): Flow<Unit> = emptyFlow()

        override suspend fun excludedGroups(entry: Entry, memberIds: Collection<Long>): Set<String> {
            if (!supported) return emptySet()
            return memberIds.map { "excluded:$it" }.toSet()
        }

        override suspend fun setExcludedGroups(entry: Entry, memberIds: Collection<Long>, excluded: Set<String>) {
            excludedRequests += memberIds.toList() to excluded
        }
    }

    private open class RecordingLibraryFilterProcessor(
        open override val type: EntryType,
        private val supported: Boolean = true,
    ) : EntryLibraryFilterProcessor {
        val requestedEntryIds = mutableListOf<Long>()

        override fun supportsOutsideReleasePeriodFilter(entry: Entry): Boolean {
            requestedEntryIds += entry.id
            return supported
        }
    }

    private class MutableTypeOpenProcessor(initialType: EntryType) : EntryOpenProcessor {
        var currentType: EntryType = initialType
        override val type: EntryType
            get() = currentType

        override fun open(context: Context, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions) = Unit

        override fun pendingIntent(
            context: Context,
            entry: Entry,
            chapter: EntryChapter,
            options: EntryOpenOptions,
        ): PendingIntent {
            return mockk(relaxed = true)
        }
    }

    private class MutableTypeContinueProcessor(initialType: EntryType) : EntryContinueProcessor {
        var currentType: EntryType = initialType
        override val type: EntryType
            get() = currentType

        override suspend fun findNext(entry: Entry): EntryChapter? = null

        override fun open(context: Context, entry: Entry, chapter: EntryChapter) = Unit
    }

    private class MutableTypeDownloadProcessor(initialType: EntryType) : RecordingDownloadProcessor(initialType) {
        var currentType: EntryType = initialType
        override val type: EntryType
            get() = currentType
    }

    private class MutableTypeConsumptionProcessor(initialType: EntryType) : RecordingConsumptionProcessor(initialType) {
        var currentType: EntryType = initialType
        override val type: EntryType
            get() = currentType
    }

    private class MutableTypeChildGroupFilterProcessor(
        initialType: EntryType,
    ) : RecordingChildGroupFilterProcessor(initialType) {
        var currentType: EntryType = initialType
        override val type: EntryType
            get() = currentType
    }

    private suspend inline fun <reified T : Throwable> assertFailsWith(
        crossinline block: suspend () -> Unit,
    ): T {
        try {
            block()
        } catch (throwable: Throwable) {
            if (throwable is T) {
                return throwable
            }
            throw throwable
        }
        throw AssertionError("Expected ${T::class.simpleName} to be thrown")
    }
}
