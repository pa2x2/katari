package mihon.entry.interactions.book.format.epub.navigation

import mihon.entry.interactions.book.format.epub.archive.EpubArchive
import mihon.entry.interactions.book.format.epub.archive.epubArchiveFile
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EpubNavigationParserTest {
    @Test
    fun `unlinked groups preserve every descendant in authored order with fragment targets`() {
        val file = epubArchiveFile(
            mapOf(
                "nav.xhtml" to """
                    <html><body><nav epub:type="toc"><ol>
                      <li><span>Part One</span><ol>
                        <li><a href="chapter.xhtml#first">First</a></li>
                        <li><span>Group</span><ol>
                          <li><a href="chapter.xhtml#second">Second</a></li>
                          <li><a href="chapter.xhtml#third">Third</a></li>
                        </ol></li>
                      </ol></li>
                      <li><a href="chapter.xhtml#fourth">Fourth</a><ol>
                        <li><a href="chapter.xhtml#detail">Detail</a></li>
                      </ol></li>
                    </ol></nav></body></html>
                """.trimIndent(),
            ),
        )
        try {
            EpubArchive(file).use { archive ->
                val items = EpubNavigationParser(archive).parse(navigationPackage(legacy = false))
                assertEquals(listOf("First", "Second", "Third", "Fourth"), items.map { it.title })
                assertEquals(listOf("first", "second", "third", "fourth"), items.map { it.target.fragments.single() })
                assertEquals(listOf("Detail"), items.last().children.map { it.title })
                assertEquals(listOf("detail"), items.last().children.single().target.fragments)
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun `prefixed NCX navigation preserves titles and fragment targets`() {
        val file = epubArchiveFile(
            mapOf(
                "toc.ncx" to """
                    <n:ncx xmlns:n="http://www.daisy.org/z3986/2005/ncx/">
                      <n:navMap><n:navPoint>
                        <n:navLabel><n:text>First</n:text></n:navLabel>
                        <n:content src="chapter.xhtml#first"/>
                      </n:navPoint></n:navMap>
                    </n:ncx>
                """.trimIndent(),
            ),
        )
        try {
            EpubArchive(file).use { archive ->
                val item = EpubNavigationParser(archive).parse(navigationPackage(legacy = true)).single()
                assertEquals("First", item.title)
                assertEquals("chapter.xhtml", item.target.resourceId)
                assertEquals(listOf("first"), item.target.fragments)
            }
        } finally {
            file.delete()
        }
    }
}
