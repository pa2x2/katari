package mihon.entry.interactions.book.content

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class SourceBookContentCatalogTest : SourceBookContentSessionFixture() {
    @Test
    fun `persisted publication identity is stable and a discriminator extends it`() {
        assertEquals("source:42:entry:/books/fixture", session(media = bookMedia()).publicationId)
        assertEquals(
            "source:42:entry:/books/fixture:publication:epub",
            session(media = bookMedia(publicationKeyOverride = "epub")).publicationId,
        )
    }

    @Test
    fun `catalog pages follow explicit resource order and otherwise the source list order`() = runTest {
        val ordered = session(
            media = bookMedia(
                resources = listOf(
                    resource("third", order = 2, location = inline("third")),
                    resource("first", order = 0, location = inline("first")),
                    resource("second", order = 1, location = inline("second")),
                ),
            ),
        )
        val firstPage = ordered.listResources(limit = 2).getOrThrow()
        val secondPage = ordered.listResources(firstPage.nextCursor, limit = 2).getOrThrow()
        assertEquals(listOf("first", "second"), firstPage.resources.map { it.id })
        assertEquals(listOf("third"), secondPage.resources.map { it.id })
        assertEquals(null, secondPage.nextCursor)

        val unordered = session(
            media = bookMedia(
                resources = listOf(
                    resource("zeta", location = inline("zeta")),
                    resource("alpha", location = inline("alpha")),
                ),
            ),
        )
        assertEquals(
            listOf("zeta", "alpha"),
            unordered.listResources(limit = 10).getOrThrow().resources.map { it.id },
        )
    }
}
