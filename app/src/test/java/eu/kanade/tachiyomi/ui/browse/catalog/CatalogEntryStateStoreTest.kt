package eu.kanade.tachiyomi.ui.browse.catalog

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.source.model.CatalogListItem

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogEntryStateStoreTest {

    @Test
    fun `loaded cards share one growing entry observation`() = runTest {
        val persistedEntries = MutableStateFlow((1L..101L).map(::entry))
        val observedIdSets = mutableListOf<List<Long>>()
        val store = CatalogEntryStateStore(backgroundScope) { entryIds ->
            observedIdSets += entryIds
            persistedEntries.map { entries -> entries.filter { it.id in entryIds } }
        }

        val states = (1L..100L).map { id -> store.stateFor(item(entry(id))) }
        states.map { it.value.id } shouldContainExactly (1L..100L).toList()

        advanceTimeBy(51)
        runCurrent()

        observedIdSets shouldContainExactly listOf((1L..100L).toList())

        persistedEntries.value = persistedEntries.value.map { entry ->
            if (entry.id == 50L) entry.copy(favorite = true, title = "Updated") else entry
        }
        runCurrent()

        (states[49].value as CatalogListItem.EntryItem).entry.run {
            favorite shouldBe true
            title shouldBe "Updated"
        }
        states[48].value.favorite shouldBe false

        store.stateFor(item(entry(101)))
        advanceTimeBy(51)
        runCurrent()

        observedIdSets shouldContainExactly listOf(
            (1L..100L).toList(),
            (1L..101L).toList(),
        )

        val reseeded = item(entry(1).copy(title = "Reseeded", favorite = true, version = 5))
        val reseededState = store.stateFor(reseeded)
        (reseededState === states.first()) shouldBe true
        reseededState.value.run {
            title shouldBe "Reseeded"
            favorite shouldBe true
        }

        persistedEntries.value = persistedEntries.value.map { entry ->
            if (entry.id == 1L) entry.copy(title = "Older observation", favorite = false, version = 4) else entry
        }
        runCurrent()
        reseededState.value.run {
            title shouldBe "Reseeded"
            favorite shouldBe true
        }

        persistedEntries.value = persistedEntries.value.map { entry ->
            if (entry.id == 1L) entry.copy(title = "Authoritative", favorite = true, version = 6) else entry
        }
        runCurrent()
        reseededState.value.run {
            title shouldBe "Authoritative"
            favorite shouldBe true
        }

        store.stateFor(item(entry(1).copy(title = "Older seed", version = 5)))
        reseededState.value.title shouldBe "Authoritative"
    }

    private fun item(entry: Entry): CatalogListItem.EntryItem {
        return CatalogListItem.EntryItem(entry, EntryItemOrientation.VERTICAL)
    }

    private fun entry(id: Long): Entry {
        return Entry.create().copy(
            id = id,
            profileId = 2,
            source = 10,
            url = "/$id",
            title = "Entry $id",
            type = EntryType.MANGA,
        )
    }
}
