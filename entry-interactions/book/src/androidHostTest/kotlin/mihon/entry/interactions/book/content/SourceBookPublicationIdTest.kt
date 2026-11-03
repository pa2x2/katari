package mihon.entry.interactions.book.content

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class SourceBookPublicationIdTest : SourceBookContentSessionFixture() {
    @Test
    fun `persisted publication identity is stable and a discriminator extends it`() {
        assertEquals("source:42:entry:/books/fixture", session(media = bookMedia()).publicationId)
        assertEquals(
            "source:42:entry:/books/fixture:publication:epub",
            session(media = bookMedia(publicationKeyOverride = "epub")).publicationId,
        )
    }
}
