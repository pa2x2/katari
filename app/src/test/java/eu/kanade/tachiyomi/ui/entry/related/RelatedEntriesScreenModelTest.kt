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
import mihon.entry.interactions.child.EntryRelatedEntriesFeature
import mihon.entry.interactions.child.EntryRelatedEntriesLoadResult
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

    private companion object {
        const val ENTRY_ID = 1L

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
