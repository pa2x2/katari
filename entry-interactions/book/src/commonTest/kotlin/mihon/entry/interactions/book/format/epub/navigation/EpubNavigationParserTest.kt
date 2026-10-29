package mihon.entry.interactions.book.format.epub.navigation

import mihon.entry.interactions.book.format.epub.archive.EpubArchive
import mihon.entry.interactions.book.format.epub.archive.epubArchiveBytesFile
import mihon.entry.interactions.book.format.epub.archive.epubArchiveFile
import mihon.entry.interactions.book.format.epub.packageinfo.EpubManifestItem
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
    fun `UTF-16 prefixed NCX navigation preserves titles and fragment targets`() {
        val file = epubArchiveBytesFile(
            mapOf(
                "toc.ncx" to """
                    <?xml version="1.0" encoding="UTF-16"?><n:ncx xmlns:n="http://www.daisy.org/z3986/2005/ncx/">
                      <n:navMap><n:navPoint>
                        <n:navLabel><n:text>Été</n:text></n:navLabel>
                        <n:content src="chapter.xhtml#first"/>
                      </n:navPoint></n:navMap>
                    </n:ncx>
                """.trimIndent().toByteArray(Charsets.UTF_16),
            ),
        )
        try {
            EpubArchive(file).use { archive ->
                val item = EpubNavigationParser(archive).parse(navigationPackage(legacy = true)).single()
                assertEquals("Été", item.title)
                assertEquals("chapter.xhtml", item.target.resourceId)
                assertEquals(listOf("first"), item.target.fragments)
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun `a cover page left out of the navigation keeps its reading-order place before the contents`() {
        val file = epubArchiveFile(
            mapOf(
                "nav.xhtml" to """
                    <html><body><nav epub:type="toc"><ol>
                      <li><a href="chapter.xhtml#first">First</a></li>
                      <li><a href="chapter.xhtml#second">Second</a></li>
                    </ol></nav></body></html>
                """.trimIndent(),
            ),
        )
        val cover = EpubManifestItem("cover", "cover.xhtml", "application/xhtml+xml", setOf("svg"))
        val afterword = EpubManifestItem("afterword", "afterword.xhtml", "application/xhtml+xml", emptySet())
        val base = navigationPackage(legacy = false)
        val chapter = base.readingOrder.single()
        val packageInfo = base.copy(
            manifest = base.manifest + (cover.id to cover) + (afterword.id to afterword),
            documents = listOf(cover, chapter, afterword),
            readingOrder = listOf(cover, chapter, afterword),
        )
        try {
            EpubArchive(file).use { archive ->
                val items = EpubNavigationParser(archive).parse(packageInfo)
                assertEquals(
                    listOf("cover.xhtml", "chapter.xhtml", "chapter.xhtml", "afterword.xhtml"),
                    items.map { it.target.resourceId },
                )
            }
        } finally {
            file.delete()
        }
    }
}
