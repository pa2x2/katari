package mihon.entry.interactions.anime.runtime

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.anime.navigation.AnimeContinueProcessor
import mihon.entry.interactions.anime.navigation.AnimeOpenProcessor
import mihon.entry.interactions.anime.state.AnimeConsumptionProcessor
import mihon.entry.interactions.anime.state.AnimeProgressProcessor
import mihon.entry.interactions.anime.state.animeProgressState
import mihon.entry.interactions.state.EntryProgressResourceMapping
import mihon.entry.interactions.state.EntryProgressSnapshot
import mihon.entry.interactions.state.EntryProgressStateSnapshot
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.service.EntryChildOwnershipResolution
import tachiyomi.domain.entry.service.EntryChildOwnershipResolutionPort

class AnimeEntryInteractionPluginTest {

    @Test
    fun `anime progress snapshot preserves portable playback position`() = runTest {
        val progressProcessor = AnimeProgressProcessor(
            FakeEntryProgressRepository(
                listOf(
                    playbackState(
                        chapterId = 10L,
                        positionMs = 12_000L,
                        durationMs = 60_000L,
                        completed = false,
                        lastWatchedAt = 123L,
                    ),
                ),
            ),
            FakeEntryChapterRepository(emptyList()),
        )

        progressProcessor.snapshot(anime()) shouldBe EntryProgressSnapshot(
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
    }

    @Test
    fun `anime progress restore maps portable child key`() = runTest {
        val progressRepository = FakeEntryProgressRepository(emptyList())
        val progressProcessor = AnimeProgressProcessor(
            entryProgressRepository = progressRepository,
            entryChapterRepository = FakeEntryChapterRepository(listOf(chapter(id = 20L, entryId = 2L))),
        )

        progressProcessor.restore(
            entry = anime(id = 2L),
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
    }

    @Test
    fun `anime progress copy maps resource state`() = runTest {
        val progressRepository = FakeEntryProgressRepository(
            listOf(
                playbackState(entryId = 1L, chapterId = 10L, positionMs = 3_000L, completed = false),
                playbackState(entryId = 1L, chapterId = 11L, positionMs = 4_000L, completed = true),
            ),
        )
        val progressProcessor =
            AnimeProgressProcessor(progressRepository, FakeEntryChapterRepository(emptyList()))

        progressProcessor.copy(
            sourceEntry = anime(id = 1L),
            targetEntry = anime(id = 2L),
            resourceMappings = listOf(
                EntryProgressResourceMapping(
                    sourceResourceKey = "/episode/10",
                    targetResourceKey = "/episode/20",
                    targetChapterId = 20L,
                ),
            ),
        )

        progressRepository.upsertedStates.shouldContainExactly(
            playbackState(entryId = 2L, chapterId = 20L, positionMs = 3_000L, completed = false),
        )
    }

    @Test
    fun `anime continue prefers first in-progress playback state by source order`() = runTest {
        val expected = chapter(id = 2L, read = false, sourceOrder = 1L)
        val processor = continueProcessor(
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

        processor.findNext(anime()) shouldBe expected
    }

    @Test
    fun `anime continue skips consumed episodes when selecting next unread`() = runTest {
        val expected = chapter(id = 2L, read = false, sourceOrder = 4L)
        val processor = continueProcessor(
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

        processor.findNext(anime()) shouldBe expected
    }

    @Test
    fun `anime consumption marks unwatched and resets watched or partial progress without changing recency`() =
        runTest {
            val playbackRepository = FakeEntryProgressRepository(
                listOf(
                    playbackState(chapterId = 1L, positionMs = 20_000L, completed = true, lastWatchedAt = 40L),
                    playbackState(chapterId = 2L, positionMs = 10_000L, completed = false, lastWatchedAt = 50L),
                ),
            )

            AnimeConsumptionProcessor(playbackRepository).setConsumed(
                entry = anime(),
                chapters = listOf(
                    chapter(id = 1L, read = true),
                    chapter(id = 2L, read = false),
                ),
                consumed = false,
            )

            playbackRepository.upsertedStates.shouldContainExactly(
                playbackState(chapterId = 1L, positionMs = 0L, durationMs = 0L, completed = false, lastWatchedAt = 40L)
                    .copy(locator = EntryProgressLocator(kind = "time")),
                playbackState(chapterId = 2L, positionMs = 0L, durationMs = 0L, completed = false, lastWatchedAt = 50L)
                    .copy(locator = EntryProgressLocator(kind = "time")),
            )
        }

    @Test
    fun `anime consumption marks watched without changing recency`() = runTest {
        val newlyWatched = chapter(id = 1L, read = false)
        val alreadyWatched = chapter(id = 2L, read = true)
        val playbackRepository = FakeEntryProgressRepository(
            listOf(
                playbackState(chapterId = 1L, positionMs = 20_000L, completed = false, lastWatchedAt = 70L),
                playbackState(chapterId = 2L, positionMs = 10_000L, completed = false),
            ),
        )

        val changed = AnimeConsumptionProcessor(playbackRepository).setConsumed(
            entry = anime(),
            chapters = listOf(newlyWatched, alreadyWatched),
            consumed = true,
        )

        playbackRepository.upsertedStates.shouldContainExactly(
            playbackState(chapterId = 1L, positionMs = 20_000L, completed = true, lastWatchedAt = 70L),
        )
        changed.shouldContainExactly(newlyWatched)
    }

    private fun continueProcessor(
        chapters: List<EntryChapter>,
        playbackStates: List<EntryProgressState> = emptyList(),
    ): AnimeContinueProcessor {
        val entries = listOf(anime())
        return AnimeContinueProcessor(
            getEntryWithChapters = GetEntryWithChapters(
                entryChapterRepository = FakeEntryChapterRepository(chapters),
                childOwnership = object : EntryChildOwnershipResolutionPort {
                    private fun resolution(profileId: Long, entryId: Long) = EntryChildOwnershipResolution(
                        profileId = profileId,
                        requestedEntryId = entryId,
                        visibleEntryId = entryId,
                        orderedOwners = entries.filter { it.id == entryId },
                    )

                    override suspend fun resolveChildOwnership(profileId: Long, entryId: Long) =
                        resolution(profileId, entryId)

                    override fun observeChildOwnership(
                        profileId: Long,
                        entryId: Long,
                    ): Flow<EntryChildOwnershipResolution> = flowOf(resolution(profileId, entryId))
                },
            ),
            entryProgressRepository = FakeEntryProgressRepository(playbackStates),
            openProcessor = AnimeOpenProcessor(),
        )
    }

    private fun anime(id: Long = 1L): Entry = Entry.create().copy(
        id = id,
        title = "Entry",
        source = 1L,
        profileId = 1L,
        type = EntryType.ANIME,
    )

    private fun chapter(
        id: Long = 1L,
        entryId: Long = 1L,
        read: Boolean = false,
        sourceOrder: Long = 0L,
        chapterNumber: Double = 0.0,
    ): EntryChapter {
        return EntryChapter.create().copy(
            id = id,
            entryId = entryId,
            url = "/episode/$id",
            name = "Episode",
            read = read,
            sourceOrder = sourceOrder,
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

    private class FakeEntryChapterRepository(
        private val chapters: List<EntryChapter>,
    ) : EntryChapterRepository {
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

        override suspend fun updateAll(chapters: List<EntryChapter>): Boolean = true

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

        override suspend fun rekey(
            entryId: Long,
            chapterId: Long?,
            oldContentKey: String,
            oldResourceKey: String,
            newContentKey: String,
            newResourceKey: String,
        ) = Unit

        private fun record(state: EntryProgressState) {
            states.removeAll { it.identity == state.identity }
            states += state
            upsertedStates += state
        }
    }
}
