package mihon.book.api.document

import kotlin.test.Test
import kotlin.test.assertEquals

class BookDocumentTextEditsTest {

    @Test
    fun `removing a paragraph and replacing words keeps links, anchors and later blocks on their text`() {
        val text = "Read at site.\n\nShe ran to the bridge.\n\nIt rained."
        val bridge = BookDocumentLink(15, 21, BookDocumentLinkTarget.Anchor("bridge"))
        val document = bookDocument(
            text = text,
            blocks = listOf(
                bookDocumentTextBlock("boilerplate", "Read at site.", 0),
                bookDocumentTextBlock("story", "She ran to the bridge.", 15).withLink(bridge),
                bookDocumentTextBlock("weather", "It rained.", 39),
            ),
            anchors = mapOf(
                "intro" to BookDocumentPosition(BookDocumentBlockId("boilerplate"), 0),
                "rain" to BookDocumentPosition(BookDocumentBlockId("weather"), 3),
            ),
        )

        val edited = document.withTextBlockEdits { block, _ ->
            when (block.id.value) {
                "boilerplate" -> listOf(BookDocumentTextEdit(0, 13, ""))
                "story" -> listOf(BookDocumentTextEdit(0, 3, "Sunny"))
                else -> emptyList()
            }
        }

        assertEquals("Sunny ran to the bridge.\n\nIt rained.", edited.content.text)
        assertEquals(listOf("story", "weather"), edited.blocks.map { it.id.value })
        val story = edited.blocks.first().content as BookDocumentBlockContent.Text
        assertEquals(listOf("bridge"), story.value.links.map { story.value.text.substring(it.start, it.endExclusive) })
        assertEquals(26, edited.blocks.last().logicalStart)
        assertEquals(
            mapOf(
                "intro" to BookDocumentPosition(BookDocumentBlockId("story"), 0),
                "rain" to BookDocumentPosition(BookDocumentBlockId("weather"), 3),
            ),
            edited.anchors,
        )
    }

    private fun BookDocumentBlock.withLink(link: BookDocumentLink): BookDocumentBlock {
        val textContent = content as BookDocumentBlockContent.Text
        return copy(content = textContent.copy(value = textContent.value.copy(links = listOf(link))))
    }
}
