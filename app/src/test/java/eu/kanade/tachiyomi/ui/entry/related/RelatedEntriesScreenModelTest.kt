package eu.kanade.tachiyomi.ui.entry.related

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
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
import mihon.entry.interactions.EntryRelatedEntriesFeature
import mihon.entry.interactions.EntryRelatedEntriesLoadResult
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import kotlin.time.Duration.Companion.seconds

class RelatedEntriesScreenModelTest {

    @Test
    fun `construction does not load related entries`() = runTest {
        val feature = mockk<EntryRelatedEntriesFeature>(relaxed = true)
        val model = RelatedEntriesScreenModel(ENTRY_ID, feature)

        try {
            model.state.value shouldBe RelatedEntriesScreenModel.State.Idle
            coVerify(exactly = 0) { feature.load(any()) }
        } finally {
            model.onDispose()
        }
    }

    @Test
    fun `explicit load resolves related entries`() = runTest {
        val related = listOf(
            entry(2L, "/manga", EntryType.MANGA),
            entry(3L, "/anime", EntryType.ANIME),
        )
        val feature = mockk<EntryRelatedEntriesFeature> {
            coEvery { load(ENTRY_ID) } returns EntryRelatedEntriesLoadResult.Loaded(
                related,
                EntryItemOrientation.HORIZONTAL,
            )
        }
        val model = RelatedEntriesScreenModel(ENTRY_ID, feature)

        try {
            model.load()

            eventually(ASYNC_TIMEOUT) {
                val state = model.state.value as RelatedEntriesScreenModel.State.Success
                state.relatedEntries.shouldContainExactly(related)
                state.sourceItemOrientation shouldBe EntryItemOrientation.HORIZONTAL
            }
            coVerify(exactly = 1) { feature.load(ENTRY_ID) }
        } finally {
            model.onDispose()
        }
    }

    @Test
    fun `retry replaces an error with loaded entries`() = runTest {
        val related = entry(2L, "/related", EntryType.MANGA)
        val feature = mockk<EntryRelatedEntriesFeature> {
            coEvery { load(ENTRY_ID) } throws IllegalStateException("First request failed") andThen
                EntryRelatedEntriesLoadResult.Loaded(
                    listOf(related),
                    EntryItemOrientation.VERTICAL,
                )
        }
        val model = RelatedEntriesScreenModel(ENTRY_ID, feature)

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
            coVerify(exactly = 2) { feature.load(ENTRY_ID) }
        } finally {
            model.onDispose()
        }
    }

    @Test
    fun `refresh retains loaded entries until replacement arrives`() = runTest {
        val initial = entry(2L, "/initial", EntryType.MANGA)
        val replacement = entry(3L, "/replacement", EntryType.ANIME)
        val refreshResult = CompletableDeferred<EntryRelatedEntriesLoadResult>()
        var requestCount = 0
        val feature = mockk<EntryRelatedEntriesFeature> {
            coEvery { load(ENTRY_ID) } coAnswers {
                requestCount++
                if (requestCount == 1) {
                    EntryRelatedEntriesLoadResult.Loaded(
                        listOf(initial),
                        EntryItemOrientation.VERTICAL,
                    )
                } else {
                    refreshResult.await()
                }
            }
        }
        val model = RelatedEntriesScreenModel(ENTRY_ID, feature)

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

            refreshResult.complete(
                EntryRelatedEntriesLoadResult.Loaded(
                    listOf(replacement),
                    EntryItemOrientation.HORIZONTAL,
                ),
            )
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
