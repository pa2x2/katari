package eu.kanade.tachiyomi.ui.browse.immersive

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.entry.VideoRequest
import eu.kanade.tachiyomi.source.entry.VideoStream
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mihon.entry.interactions.EntryChildListInteraction
import mihon.entry.interactions.EntryImmersiveHandle
import mihon.entry.interactions.EntryImmersiveInteraction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.interactor.SyncEntryWithSource
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.source.service.SourceManager
import kotlin.time.Duration.Companion.seconds

class EntryImmersiveScreenModelTest {

    @Test
    fun `retain evicts ready handles outside bounded window`() = runTest {
        val fixture = fixture()
        val manga = entry(id = 1L, type = EntryType.MANGA)
        val anime = entry(id = 2L, type = EntryType.ANIME)
        val model = fixture.model

        try {
            model.load(fixture.context, manga)
            model.load(fixture.context, anime)
            eventually(ASYNC_TIMEOUT) { model.state.value.items.size shouldBe 2 }

            model.retain(setOf(EntryImmersiveItemKey(anime.id, anime.type)))

            eventually(ASYNC_TIMEOUT) {
                model.state.value.items.keys shouldBe setOf(EntryImmersiveItemKey(anime.id, anime.type))
            }
            verify(exactly = 1) {
                fixture.interaction.release(
                    fixture.handles.getValue(EntryImmersiveItemKey(1L, EntryType.MANGA)),
                )
            }
        } finally {
            model.onDispose()
        }
    }

    @Test
    fun `same numeric id with different type remains distinct`() = runTest {
        val fixture = fixture()
        val manga = entry(id = 7L, type = EntryType.MANGA)
        val anime = entry(id = 7L, type = EntryType.ANIME)

        try {
            fixture.model.load(fixture.context, manga)
            fixture.model.load(fixture.context, anime)

            eventually(ASYNC_TIMEOUT) {
                fixture.model.state.value.items.keys.shouldContainExactlyInAnyOrder(
                    EntryImmersiveItemKey(7L, EntryType.MANGA),
                    EntryImmersiveItemKey(7L, EntryType.ANIME),
                )
            }
        } finally {
            fixture.model.onDispose()
        }
    }

    private fun fixture(): Fixture {
        val context = mockk<Context>(relaxed = true)
        val chapter = EntryChapter.create().copy(id = 20L, entryId = 1L)
        val repository = mockk<EntryChapterRepository> {
            coEvery { getChaptersByEntryIdAwait(any(), any()) } returns listOf(chapter)
        }
        val childList = mockk<EntryChildListInteraction>(relaxed = true) {
            every { sortedForReading(any(), any(), any()) } answers { secondArg<List<EntryChapter>>() }
        }
        val handles = mutableMapOf<EntryImmersiveItemKey, EntryImmersiveHandle>()
        val interaction = mockk<EntryImmersiveInteraction>(relaxed = true) {
            every { isSupported(any()) } returns true
            coEvery { load(any(), any(), any(), any()) } answers {
                val loadedEntry = secondArg<Entry>()
                val ref = EntryImmersiveItemKey(loadedEntry.id, loadedEntry.type)
                handles.getOrPut(ref) {
                    when (loadedEntry.type) {
                        EntryType.MANGA -> EntryImmersiveHandle.ImagePages(
                            entryType = loadedEntry.type,
                            chapterId = chapter.id,
                            delegate = Unit,
                        )
                        EntryType.ANIME -> EntryImmersiveHandle.Playback(
                            entryType = loadedEntry.type,
                            chapterId = chapter.id,
                            stream = VideoStream(VideoRequest("https://example.invalid/video")),
                            subtitles = emptyList(),
                            resumePositionMs = 0L,
                        )
                    }
                }
            }
        }
        val source = mockk<UnifiedSource>(relaxed = true)
        val sourceManager = mockk<SourceManager>()
        every { sourceManager.get(any()) } returns source
        return Fixture(
            context = context,
            interaction = interaction,
            handles = handles,
            model = EntryImmersiveScreenModel(
                entryChapterRepository = repository,
                syncEntryWithSource = mockk<SyncEntryWithSource>(relaxed = true),
                childListInteraction = childList,
                immersiveInteraction = interaction,
                sourceManager = sourceManager,
            ),
        )
    }

    private fun entry(id: Long, type: EntryType): Entry {
        return Entry.create().copy(id = id, source = 1L, type = type)
    }

    private data class Fixture(
        val context: Context,
        val interaction: EntryImmersiveInteraction,
        val handles: Map<EntryImmersiveItemKey, EntryImmersiveHandle>,
        val model: EntryImmersiveScreenModel,
    )

    companion object {
        private val ASYNC_TIMEOUT = 5.seconds

        @OptIn(DelicateCoroutinesApi::class)
        private val mainThread = newSingleThreadContext("EntryImmersiveScreenModelTest")

        @JvmStatic
        @BeforeAll
        @OptIn(ExperimentalCoroutinesApi::class)
        fun setUpMainDispatcher() {
            Dispatchers.setMain(mainThread)
        }

        @JvmStatic
        @AfterAll
        @OptIn(ExperimentalCoroutinesApi::class)
        fun tearDownMainDispatcher() {
            Dispatchers.resetMain()
            mainThread.close()
        }
    }
}
