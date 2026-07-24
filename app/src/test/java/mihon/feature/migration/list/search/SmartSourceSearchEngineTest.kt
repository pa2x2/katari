package mihon.feature.migration.list.search

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.EntryCatalogueFeature
import mihon.entry.interactions.EntryCatalogueSearchRequest
import mihon.entry.interactions.EntryCatalogueSearchResult
import mihon.entry.interactions.EntryCatalogueSourceInfo
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class SmartSourceSearchEngineTest {
    @Test
    fun `regular search requests only candidates matching requested entry type`() = runTest {
        val source = EntryCatalogueSourceInfo(
            id = 1L,
            name = "Mixed Source",
            language = "en",
            supportedEntryTypes = setOf(EntryType.MANGA, EntryType.ANIME),
            itemOrientation = EntryItemOrientation.VERTICAL,
            supportsLatest = true,
            usesAsyncFilters = false,
        )
        val feature = mockk<EntryCatalogueFeature> {
            coEvery { search(any()) } answers {
                val request = firstArg<EntryCatalogueSearchRequest>()
                EntryCatalogueSearchResult.Success(
                    listOf(
                        Entry.create().copy(
                            source = request.sourceId,
                            url = "/same",
                            title = "Same",
                            type = checkNotNull(request.requiredType),
                        ),
                    ),
                )
            }
        }
        val engine = SmartSourceSearchEngine(extraSearchParams = null, catalogueFeature = feature)

        engine.regularSearch(source, "Same", EntryType.ANIME)?.type shouldBe EntryType.ANIME
        engine.regularSearch(source, "Same", EntryType.MANGA)?.type shouldBe EntryType.MANGA
    }
}
