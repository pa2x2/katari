package mihon.entry.interactions.book.content

import eu.kanade.tachiyomi.source.entry.BookResourceHierarchyNode
import kotlinx.coroutines.test.runTest
import mihon.book.api.BookCatalogCoverage
import mihon.book.api.BookResourceCacheState
import mihon.book.api.BookResourceCapability
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
internal class SourceBookContentCatalogTest : SourceBookContentSessionFixture() {
    @Test
    fun `catalog paging preserves source ordering identity and publication revision`() = runTest {
        val session = session(
            media = bookMedia(
                resources = listOf(
                    resource("third", order = 2, location = inline("third")),
                    resource(
                        "first",
                        title = "First chapter",
                        order = 0,
                        groupId = "volume-1",
                        location = inline("first"),
                    ),
                    resource("second", order = 1, location = inline("second")),
                ),
                initialResourceId = "first",
                initialLocation = inline("first"),
                hierarchy = listOf(
                    BookResourceHierarchyNode(
                        id = "volume-1",
                        title = "Volume 1",
                        resourceIds = listOf("first", "second"),
                    ),
                ),
            ),
        )

        val firstPage = session.listResources(limit = 2).getOrThrow()
        val secondPage = session.listResources(firstPage.nextCursor, limit = 2).getOrThrow()

        assertEquals("source:42:entry:/books/fixture", session.publicationId)
        assertEquals("publication-v2", session.revision)
        assertEquals(listOf("en"), session.languages)
        assertEquals("catalog-v3", session.catalogRevision)
        assertEquals(BookCatalogCoverage.COMPLETE, session.catalogCoverage)
        assertEquals("volume-1", session.resourceHierarchy.single().id)
        assertEquals(listOf("first", "second"), session.resourceHierarchy.single().resourceIds)
        assertEquals(listOf("first"), session.primaryResourceIds)
        assertEquals(listOf("first", "second"), firstPage.resources.map { it.id })
        assertEquals(listOf("third"), secondPage.resources.map { it.id })
        assertEquals(null, secondPage.nextCursor)
        assertEquals(BookResourceCacheState.CACHED, firstPage.resources.first().cacheState)
        assertEquals("First chapter", firstPage.resources.first().title)
        assertEquals(0L, firstPage.resources.first().order)
        assertEquals("volume-1", firstPage.resources.first().groupId)
        assertEquals(
            setOf(
                BookResourceCapability.STREAM,
                BookResourceCapability.RANGE,
                BookResourceCapability.MATERIALIZE,
            ),
            firstPage.resources.first().capabilities,
        )
    }

    @Test
    fun `publication discriminator extends rather than replaces default identity`() {
        val session = session(media = bookMedia(publicationKeyOverride = "epub"))

        assertEquals("source:42:entry:/books/fixture:publication:epub", session.publicationId)
    }

    @Test
    fun `catalog preserves source list order when explicit ordering is absent`() = runTest {
        val session = session(
            media = bookMedia(
                resources = listOf(
                    resource("zeta", location = inline("zeta")),
                    resource("alpha", location = inline("alpha")),
                ),
            ),
        )

        assertEquals(
            listOf("zeta", "alpha"),
            session.listResources(limit = 10).getOrThrow().resources.map { it.id },
        )
        assertTrue(session.listResources(cursor = "invalid", limit = 10).isFailure)
        assertTrue(session.listResources(limit = 501).isFailure)
    }
}
