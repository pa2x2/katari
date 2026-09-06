package eu.kanade.tachiyomi.source.filter

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.filter.EntryDateFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterMetadata
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterValidationCode
import eu.kanade.tachiyomi.source.entry.filter.validationIssues
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
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
        val first = async {
            draft.withSourceFilterValues {
                entered.complete(Unit)
                finish.await()
                source.state shouldBe 1
            }
        }
        entered.await()
        val second = async { other.withSourceFilterValues { source.state shouldBe 0 } }
        finish.complete(Unit)
        first.await()
        second.await()
        source.state shouldBe 0
        try {
            draft.withSourceFilterValues<Unit> { error("request failed") }
        } catch (_: IllegalStateException) { }
        source.state shouldBe 0
    }

    @Test
    fun `cancelled source callbacks restore defaults and release the shared binding`() = runTest {
        val source = ProviderChoice()
        val draft = EntryFilterList(source).detachedCopy()
        (draft.single() as EntryFilter.Select<*>).state = 1
        val entered = CompletableDeferred<Unit>()
        val request = launch {
            draft.withSourceFilterValues {
                entered.complete(Unit)
                awaitCancellation()
            }
        }
        entered.await()
        request.cancelAndJoin()
        source.state shouldBe 0
        draft.withSourceFilterValues { source.state shouldBe 1 }
        source.state shouldBe 0
    }

    @Test
    fun `date validators see projected editor values without changing the source`() {
        val source = EntryDateFilter("Start", EntryFilterMetadata(id = "start"))
        val projection = EntryFilterList(source).detachedCopy()
        (projection.single() as EntryDateFilter).state = "2023-02-29"
        projection.validationIssues().single().code shouldBe EntryFilterValidationCode.INVALID_DATE
        (projection.single() as EntryDateFilter).validateFilter().single().code shouldBe
            EntryFilterValidationCode.INVALID_DATE
        source.state shouldBe ""
        source.validateFilter().isEmpty() shouldBe true
    }
}
