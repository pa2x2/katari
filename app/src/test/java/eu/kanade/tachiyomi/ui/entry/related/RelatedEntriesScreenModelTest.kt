package eu.kanade.tachiyomi.ui.entry.related

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.interactor.GetRelatedEntries
import tachiyomi.domain.entry.model.Entry
import kotlin.time.Duration.Companion.seconds

class RelatedEntriesScreenModelTest {

    @Test
    fun `construction does not load related entries`() = runTest {
        val getEntry = mockk<GetEntry>(relaxed = true)
        val getRelatedEntries = mockk<GetRelatedEntries>(relaxed = true)
        val model = RelatedEntriesScreenModel(ENTRY_ID, getEntry, getRelatedEntries)

        try {
            model.state.value shouldBe RelatedEntriesScreenModel.State.Idle
            coVerify(exactly = 0) { getEntry.await(any()) }
            coVerify(exactly = 0) { getRelatedEntries.await(any()) }
        } finally {
            model.onDispose()
        }
    }

    @Test
    fun `explicit load resolves related entries`() = runTest {
        val origin = entry(ENTRY_ID, "/origin", EntryType.BOOK)
        val related = listOf(
            entry(2L, "/manga", EntryType.MANGA),
            entry(3L, "/anime", EntryType.ANIME),
        )
        val getEntry = mockk<GetEntry> {
            coEvery { await(ENTRY_ID) } returns origin
        }
        val getRelatedEntries = mockk<GetRelatedEntries> {
            coEvery { await(origin) } returns related
        }
        val model = RelatedEntriesScreenModel(ENTRY_ID, getEntry, getRelatedEntries)

        try {
            model.load()

            eventually(ASYNC_TIMEOUT) {
                val state = model.state.value as RelatedEntriesScreenModel.State.Success
                state.entry shouldBe origin
                state.relatedEntries.shouldContainExactly(related)
            }
            coVerify(exactly = 1) { getRelatedEntries.await(origin) }
        } finally {
            model.onDispose()
        }
    }

    @Test
    fun `retry replaces an error with loaded entries`() = runTest {
        val origin = entry(ENTRY_ID, "/origin", EntryType.MANGA)
        val related = entry(2L, "/related", EntryType.MANGA)
        val getEntry = mockk<GetEntry> {
            coEvery { await(ENTRY_ID) } returns origin
        }
        val getRelatedEntries = mockk<GetRelatedEntries> {
            coEvery { await(origin) } throws IllegalStateException("First request failed") andThen listOf(related)
        }
        val model = RelatedEntriesScreenModel(ENTRY_ID, getEntry, getRelatedEntries)

        try {
            model.load()
            eventually(ASYNC_TIMEOUT) {
                (model.state.value is RelatedEntriesScreenModel.State.Error) shouldBe true
            }

            model.retry()
            eventually(ASYNC_TIMEOUT) {
                val state = model.state.value as RelatedEntriesScreenModel.State.Success
                state.relatedEntries shouldContainExactly listOf(related)
            }
            coVerify(exactly = 2) { getRelatedEntries.await(origin) }
        } finally {
            model.onDispose()
        }
    }

    @Test
    fun `refresh retains loaded entries until replacement arrives`() = runTest {
        val origin = entry(ENTRY_ID, "/origin", EntryType.MANGA)
        val initial = entry(2L, "/initial", EntryType.MANGA)
        val replacement = entry(3L, "/replacement", EntryType.ANIME)
        val refreshResult = CompletableDeferred<List<Entry>>()
        var requestCount = 0
        val getEntry = mockk<GetEntry> {
            coEvery { await(ENTRY_ID) } returns origin
        }
        val getRelatedEntries = mockk<GetRelatedEntries> {
            coEvery { await(origin) } coAnswers {
                requestCount++
                if (requestCount == 1) listOf(initial) else refreshResult.await()
            }
        }
        val model = RelatedEntriesScreenModel(ENTRY_ID, getEntry, getRelatedEntries)

        try {
            model.load()
            eventually(ASYNC_TIMEOUT) {
                val state = model.state.value as RelatedEntriesScreenModel.State.Success
                state.relatedEntries shouldContainExactly listOf(initial)
                state.isRefreshing shouldBe false
            }

            model.refresh()
            eventually(ASYNC_TIMEOUT) {
                val state = model.state.value as RelatedEntriesScreenModel.State.Success
                state.relatedEntries shouldContainExactly listOf(initial)
                state.isRefreshing shouldBe true
            }

            refreshResult.complete(listOf(replacement))
            eventually(ASYNC_TIMEOUT) {
                val state = model.state.value as RelatedEntriesScreenModel.State.Success
                state.relatedEntries shouldContainExactly listOf(replacement)
                state.isRefreshing shouldBe false
            }
        } finally {
            model.onDispose()
        }
    }

    private fun entry(id: Long, url: String, type: EntryType): Entry = Entry.create().copy(
        id = id,
        source = 7L,
        url = url,
        title = url,
        type = type,
    )

    private companion object {
        const val ENTRY_ID = 1L
        val ASYNC_TIMEOUT = 5.seconds

        @OptIn(DelicateCoroutinesApi::class)
        val mainThread = newSingleThreadContext("RelatedEntriesScreenModelTest")

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
