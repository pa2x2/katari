package mihon.entry.interactions

import androidx.paging.PagingSource
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryPageResult
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.SEntry
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.validation.productionSubjectEvaluation
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.source.model.CatalogListItem
import tachiyomi.domain.source.model.EntryCatalogueDescription
import tachiyomi.domain.source.model.EntrySourceDescription

class EntryCatalogueFeatureTest {
    private val description = EntrySourceDescription(
        language = "en",
        supportedEntryTypes = setOf(EntryType.MANGA, EntryType.ANIME),
        itemOrientation = EntryItemOrientation.HORIZONTAL,
        catalogue = EntryCatalogueDescription(supportsLatest = true),
    )
    private val source = EntryCatalogueHostSource(7L, "Source", description, usesAsyncFilters = true)

    @Test
    fun `source discovery and resolution expose Feature-owned catalogue facts`() {
        val host = host()
        every { host.sources() } returns listOf(source)
        every { host.source(7L) } returns EntryCatalogueHostSourceResolution.Available(source)
        every { host.source(8L) } returns EntryCatalogueHostSourceResolution.Unsupported
        every { host.source(9L) } returns EntryCatalogueHostSourceResolution.Missing

        val feature = feature(host)

        feature.sources() shouldBe listOf(source.toInfo())
        feature.source(7L) shouldBe EntryCatalogueSourceResolution.Available(source.toInfo())
        feature.source(8L) shouldBe EntryCatalogueSourceResolution.Unsupported(8L)
        feature.source(9L) shouldBe EntryCatalogueSourceResolution.Missing(9L)
    }

    @Test
    fun `filters normalize provider availability and failures`() = runTest {
        val filters = EntryFilterList()
        val failure = IllegalStateException("filters failed")
        val host = host()
        every { host.source(7L) } returns EntryCatalogueHostSourceResolution.Available(source)
        every { host.source(8L) } returns EntryCatalogueHostSourceResolution.Unsupported
        coEvery { host.filters(7L) } returns filters andThenThrows failure

        val feature = feature(host)

        feature.filters(7L) shouldBe EntryCatalogueFiltersResult.Available(filters)
        feature.filters(7L) shouldBe EntryCatalogueFiltersResult.Failed(failure)
        feature.filters(8L) shouldBe
            EntryCatalogueFiltersResult.Unavailable(EntryCatalogueUnavailableReason.CATALOGUE_UNSUPPORTED)
    }

    @Test
    fun `background search filters provider results by type without persisting candidates`() = runTest {
        val filters = EntryFilterList()
        val manga = sourceEntry("/same", EntryType.MANGA)
        val duplicate = sourceEntry("/same", EntryType.MANGA)
        val anime = sourceEntry("/anime", EntryType.ANIME)
        val host = host()
        every { host.source(7L) } returns EntryCatalogueHostSourceResolution.Available(source)
        every { host.backgroundFilters(7L) } returns filters
        coEvery { host.page(7L, 1, any<EntryCatalogueListing.Search>()) } returns
            EntryPageResult(listOf(manga, duplicate, anime), false)
        val networkToLocal = mockk<NetworkToLocalEntry>()

        val result = feature(host, networkToLocal).search(
            EntryCatalogueSearchRequest(7L, "query", requiredType = EntryType.MANGA),
        )

        result.shouldBeInstanceOf<EntryCatalogueSearchResult.Success>().entries.map { it.url } shouldBe listOf("/same")
        io.mockk.coVerify(exactly = 0) { networkToLocal.invoke(any<List<Entry>>()) }
    }

    @Test
    fun `paging persists unique entries and retains orientation and page keys`() = runTest {
        val first = sourceEntry("/first", EntryType.BOOK)
        val second = sourceEntry("/second", EntryType.BOOK)
        val host = host()
        every { host.source(7L) } returns EntryCatalogueHostSourceResolution.Available(source)
        coEvery { host.page(7L, 1, EntryCatalogueListing.Popular) } returns
            EntryPageResult(listOf(first, first.copy(), second), true)
        val networkToLocal = mockk<NetworkToLocalEntry> {
            coEvery { this@mockk.invoke(any<List<Entry>>()) } answers { firstArg() }
        }

        val result = feature(host, networkToLocal)
            .paging(EntryCatalogueBrowseRequest(7L, EntryCatalogueListing.Popular))
            .load(PagingSource.LoadParams.Refresh(key = null, loadSize = 25, placeholdersEnabled = false))

        val page = result.shouldBeInstanceOf<PagingSource.LoadResult.Page<Long, CatalogListItem>>()
        page.data.map { (it as CatalogListItem.EntryItem).entry.url } shouldBe listOf("/first", "/second")
        page.data.map { (it as CatalogListItem.EntryItem).sourceItemOrientation }.distinct() shouldBe
            listOf(EntryItemOrientation.HORIZONTAL)
        page.nextKey shouldBe 2L
    }

    @Test
    fun `provider cancellation remains cancellation`() = runTest {
        val host = host()
        every { host.source(7L) } returns EntryCatalogueHostSourceResolution.Available(source)
        every { host.backgroundFilters(7L) } returns EntryFilterList()
        coEvery { host.page(any(), any(), any()) } throws CancellationException()

        org.junit.jupiter.api.assertThrows<CancellationException> {
            feature(host).search(EntryCatalogueSearchRequest(7L, "query"))
        }
    }

    private fun feature(
        host: EntryCatalogueProviderHost,
        networkToLocalEntry: NetworkToLocalEntry = mockk(),
    ): EntryCatalogueFeature {
        val evaluation = productionSubjectEvaluation(EntryType.BOOK, EntryCatalogueFeatureContributor)
        return DefaultEntryCatalogueFeature(
            host = host,
            graphStateValidator = EntryCatalogueGraphStateValidator(evaluation),
            networkToLocalEntry = networkToLocalEntry,
        )
    }

    private fun host() = mockk<EntryCatalogueProviderHost> {
        every { isInitialized } returns MutableStateFlow(true)
    }

    private fun EntryCatalogueHostSource.toInfo() = EntryCatalogueSourceInfo(
        id = id,
        name = name,
        language = description.language,
        supportedEntryTypes = description.supportedEntryTypes,
        itemOrientation = description.itemOrientation,
        supportsLatest = description.catalogue?.supportsLatest == true,
        usesAsyncFilters = usesAsyncFilters,
    )

    private fun sourceEntry(url: String, type: EntryType): SEntry = SEntry.create().apply {
        this.url = url
        title = url
        this.type = type
    }
}
