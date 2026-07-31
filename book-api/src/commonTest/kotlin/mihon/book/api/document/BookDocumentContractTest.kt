package mihon.book.api.document

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BookDocumentContractTest {

    @Test
    fun `logical offsets preserve exact block boundaries and canonical gaps`() {
        val first = bookDocumentTextBlock("first", "First", logicalStart = 0)
        val second = bookDocumentTextBlock("second", "Second", logicalStart = 7)
        val document = bookDocument(
            text = "First\n\nSecond",
            blocks = listOf(first, second),
        )

        assertEquals(
            BookDocumentPosition(second.id, 0),
            document.positionAtLogicalOffset(7),
        )
        assertEquals(
            BookDocumentPosition(second.id, second.logicalLength),
            document.positionAtLogicalOffset(document.logicalExtent),
        )
    }

    @Test
    fun `rich leaves must reproduce their canonical block substring`() {
        val invalid = bookDocumentTextBlock("block", "Different", logicalStart = 0).copy(
            content = BookDocumentBlockContent.Text(
                BookDocumentRichText(
                    text = "Mismatch",
                    range = BookDocumentTextRange(0, "Mismatch".length),
                ),
            ),
        )

        assertFailsWith<IllegalArgumentException> {
            bookDocument(text = "Different", blocks = listOf(invalid))
        }
    }

    @Test
    fun `text content must cover its complete canonical block`() {
        val invalid = bookDocumentTextBlock("block", "abc", logicalStart = 0).copy(
            content = BookDocumentBlockContent.Text(
                BookDocumentRichText(
                    text = "b",
                    range = BookDocumentTextRange(1, 2),
                ),
            ),
        )

        assertFailsWith<IllegalArgumentException> {
            bookDocument(text = "abc", blocks = listOf(invalid))
        }
    }

    @Test
    fun `plain text is a trimmed search projection rather than a location source`() {
        assertFailsWith<IllegalArgumentException> {
            bookDocumentTextBlock("block", "Canonical\n\n", logicalStart = 0).copy(
                plainText = "Canonical\n",
            )
        }
    }

    @Test
    fun `source fragments must be unique across canonical content`() {
        val first = bookDocumentTextBlock("first", "First", logicalStart = 0).copy(
            sourceFragments = listOf("duplicate"),
        )
        val second = bookDocumentTextBlock("second", "Second", logicalStart = 7).copy(
            sourceFragments = listOf("duplicate"),
        )

        assertFailsWith<IllegalArgumentException> {
            bookDocument(
                text = "First\n\nSecond",
                blocks = listOf(first, second),
            )
        }
    }

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
