package mihon.entry.interactions.book.format.epub.archive

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class EpubArchiveReferenceTest {
    @Test
    fun `escaped Unicode preserves identity and references are decoded exactly once`() {
        val expected = EpubArchiveReference.Internal("OPS/章😀.xhtml", "位置😀")
        listOf(
            "章😀.xhtml#位置😀",
            "%E7%AB%A0%F0%9F%98%80.xhtml#%E4%BD%8D%E7%BD%AE%F0%9F%98%80",
            "章%F0%9F%98%80.xhtml#位置%F0%9F%98%80",
        ).forEach { reference ->
            assertEquals(expected, resolveArchiveReference("OPS/package.opf", reference))
        }
        assertEquals(
            EpubArchiveReference.Internal("OPS/chapter+%20.xhtml", "part+%23"),
            resolveArchiveReference("OPS/package.opf", "chapter+%2520.xhtml?version=1#part%2B%2523"),
        )
    }

    @Test
    fun `escaped traversal remains confined and malformed URI escapes are rejected`() {
        assertEquals(
            EpubArchiveReference.Internal("chapter.xhtml", null),
            resolveArchiveReference("OPS/package.opf", "%2E%2E/chapter.xhtml"),
        )
        assertFailsWith<IllegalArgumentException> {
            resolveArchiveReference("OPS/package.opf", "%2E%2E/%2E%2E/chapter.xhtml")
        }
        assertNull(resolveArchiveReference("OPS/package.opf", "chapter%ZZ.xhtml"))
    }
}
