package eu.kanade.tachiyomi.source.filter

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DetachedEntryFiltersTest {
    private class ProviderChoice : EntryFilter.Select<String>("Type", arrayOf("Any", "Movie"))

    @Test
    fun `draft edits cannot change applied values or the providers concrete filters`() = runTest {
        val source = ProviderChoice()
        val draft = EntryFilterList(source).detachedCopy()
        val applied = draft.detachedCopy()
        (draft.single() as EntryFilter.Select<*>).state = 1
        applied.withSourceFilterValues { values ->
            (values.single() is ProviderChoice) shouldBe true
            (values.single() as ProviderChoice).state shouldBe 0
        }
        draft.withSourceFilterValues { (it.single() as ProviderChoice).state shouldBe 1 }
        source.state shouldBe 0
    }

    @Test
    fun `requests sharing cached filters serialize state and restore it on failure`() = runTest {
        val source = ProviderChoice()
        val draft = EntryFilterList(source).detachedCopy()
        val other = draft.detachedCopy()
        (draft.single() as EntryFilter.Select<*>).state = 1
        val entered = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val secondEntered = CompletableDeferred<Unit>()
        val first = async {
            draft.withSourceFilterValues {
                entered.complete(Unit)
                finish.await()
                source.state shouldBe 1
            }
        }
        entered.await()
        val second = async(start = CoroutineStart.UNDISPATCHED) {
            other.withSourceFilterValues {
                secondEntered.complete(Unit)
                source.state shouldBe 0
            }
        }
        secondEntered.isCompleted shouldBe false
        finish.complete(Unit)
        first.await()
        second.await()
        source.state shouldBe 0
        assertThrows<IllegalStateException> {
            draft.withSourceFilterValues<Unit> { error("request failed") }
        }
        source.state shouldBe 0
    }
}
