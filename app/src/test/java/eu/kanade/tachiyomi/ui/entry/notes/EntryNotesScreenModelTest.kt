package eu.kanade.tachiyomi.ui.entry.notes

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
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
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository
import java.util.Collections
import kotlin.time.Duration.Companion.seconds

class EntryNotesScreenModelTest {
    @Test
    fun `final edit is accepted and drained after screen model disposal`() = runTest {
        val writes = Collections.synchronizedList(mutableListOf<String>())
        val firstWriteStarted = CompletableDeferred<Unit>()
        val releaseFirstWrite = CompletableDeferred<Unit>()
        val repository = mockk<EntryRepository> {
            coEvery { updateNotes(ENTRY_ID, PROFILE_ID, any()) } coAnswers {
                val notes = args[2] as String
                writes += notes
                if (notes == "first") {
                    firstWriteStarted.complete(Unit)
                    releaseFirstWrite.await()
                }
                true
            }
        }
        val model = EntryNotesScreenModel(entry(), repository)

        model.updateNotes("first")
        firstWriteStarted.await()
        model.updateNotes("intermediate")
        model.onDispose()
        model.finishEditing("final")
        releaseFirstWrite.complete(Unit)

        model.state.value.notes shouldBe "final"
        eventually(ASYNC_TIMEOUT) {
            writes.shouldContainExactly("first", "final")
        }
    }

    private fun entry(): Entry = Entry.create().copy(
        id = ENTRY_ID,
        profileId = PROFILE_ID,
        source = 10,
        url = "entry",
        title = "Entry",
        notes = "initial",
    )

    private companion object {
        const val ENTRY_ID = 1L
        const val PROFILE_ID = 4L
        val ASYNC_TIMEOUT = 5.seconds

        @OptIn(DelicateCoroutinesApi::class)
        val mainThread = newSingleThreadContext("EntryNotesScreenModelTest")

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
