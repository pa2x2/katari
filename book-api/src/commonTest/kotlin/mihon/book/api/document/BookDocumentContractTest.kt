package mihon.book.api.document

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BookDocumentContractTest {

    @Test
    fun `resource ids must exactly match modeled image font and nested resources`() {
        val text = "Alt"
        val block = BookDocumentBlock(
            id = BookDocumentBlockId("figure"),
            role = BookDocumentBlockRole(BookDocumentBlockKind.FIGURE),
            content = BookDocumentBlockContent.Figure(
                image = BookDocumentImage(
                    resourceId = "image",
                    alternativeText = BookDocumentRichText(
                        text = text,
                        range = BookDocumentTextRange(0, text.length),
                        inlineStyles = listOf(
                            BookDocumentInlineStyleRange(
                                start = 0,
                                endExclusive = text.length,
                                style = BookDocumentInlineStyle(
                                    fontFamily = BookDocumentFontFamily.Resource("font"),
                                ),
                            ),
                        ),
                    ),
                    width = null,
                    height = null,
                ),
                caption = null,
            ),
            plainText = text,
            sourceFragments = emptyList(),
            logicalStart = 0,
            logicalEndExclusive = text.length,
        )

        assertFailsWith<IllegalArgumentException> {
            bookDocument(
                text = text,
                blocks = listOf(block),
                resourceIds = setOf("image"),
            )
        }
        assertEquals(
            setOf("image", "font"),
            bookDocument(
                text = text,
                blocks = listOf(block),
                resourceIds = setOf("image", "font"),
            ).resourceIds,
        )
    }

    @Test
    fun `disclosure body keeps its own canonical text anchors and resources`() {
        val bodyText = "Nested body"
        val bodyBlock = bookDocumentTextBlock("body", bodyText, logicalStart = 0).copy(
            content = BookDocumentBlockContent.Text(
                BookDocumentRichText(
                    text = bodyText,
                    range = BookDocumentTextRange(0, bodyText.length),
                    links = listOf(
                        BookDocumentLink(
                            start = 0,
                            endExclusive = "Nested".length,
                            target = BookDocumentLinkTarget.Anchor("nested"),
                        ),
                    ),
                    inlineStyles = listOf(
                        BookDocumentInlineStyleRange(
                            start = 0,
                            endExclusive = "Nested".length,
                            style = BookDocumentInlineStyle(italic = true),
                        ),
                    ),
                ),
            ),
            style = BookDocumentStyle(
                fontFamily = BookDocumentFontFamily.Resource("nested-font"),
            ),
        )
        val body = BookDocumentContent(
            text = "Nested body",
            blocks = listOf(bodyBlock),
            anchors = mapOf("nested" to BookDocumentPosition(bodyBlock.id, 7)),
            resourceIds = setOf("nested-font"),
        )
        val summary = "Summary"
        val canonical = "$summary\n${body.text}"
        val disclosure = BookDocumentBlock(
            id = BookDocumentBlockId("disclosure"),
            role = BookDocumentBlockRole(BookDocumentBlockKind.DISCLOSURE),
            content = BookDocumentBlockContent.Disclosure(
                summary = BookDocumentRichText(
                    text = summary,
                    range = BookDocumentTextRange(0, summary.length),
                ),
                body = body,
                bodyStartWithinBlock = summary.length + 1,
                initiallyExpanded = false,
            ),
            plainText = canonical,
            sourceFragments = emptyList(),
            logicalStart = 0,
            logicalEndExclusive = canonical.length,
        )

        val document = bookDocument(
            text = canonical,
            blocks = listOf(disclosure),
            anchors = mapOf(
                "nested" to BookDocumentPosition(
                    disclosure.id,
                    summary.length + 1 + 7,
                ),
            ),
            resourceIds = body.resourceIds,
        )

        val content = document.blocks.single().content as BookDocumentBlockContent.Disclosure
        assertEquals("Nested body", content.body.text)
        assertEquals(setOf("nested-font"), content.body.resourceIds)
        assertEquals(BookDocumentPosition(bodyBlock.id, 7), content.body.anchors["nested"])
        assertEquals(
            BookDocumentPosition(disclosure.id, summary.length + 1 + 7),
            document.anchors["nested"],
        )
        assertEquals(summary.length + 1, document.blocks.single().links.single().start)
        assertEquals(summary.length + 1, document.blocks.single().inlineStyles.single().start)
    }
}
