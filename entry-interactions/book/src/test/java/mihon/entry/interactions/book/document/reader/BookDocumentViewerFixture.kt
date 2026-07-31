package mihon.entry.interactions.book.document.reader

import android.text.SpannableString
import mihon.book.api.document.BookDocument
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentBlockId
import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentBlockRole
import mihon.book.api.document.BookDocumentContent
import mihon.book.api.document.BookDocumentPosition
import mihon.book.api.document.BookDocumentRichText
import mihon.book.api.document.BookDocumentTextRange
import mihon.entry.interactions.book.document.render.PreparedBookDocument
import mihon.entry.interactions.book.document.render.PreparedBookDocumentBlock
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
internal abstract class BookDocumentViewerFixture {
    protected fun assertRestorationRoundTrip(offsetWithinBlock: Int) {
        val section = section("current", listOf("a".repeat(100)))
        val item = BookDocumentViewerItem.Block(section, section.document.blocks.single())
        val viewportStartOffset = 0
        val viewportEndOffset = 800
        val itemSize = 1_000
        val scrollOffset = bookDocumentScrollOffset(
            document = section.document,
            position = BookDocumentPosition(item.content.block.id, offsetWithinBlock),
            itemSize = itemSize,
            viewportStartOffset = viewportStartOffset,
            viewportEndOffset = viewportEndOffset,
        )

        val restored = bookDocumentViewerLocation(
            items = listOf(item),
            visibleItems = listOf(
                BookDocumentVisibleItemLayout(
                    index = 0,
                    key = item.key,
                    offset = -scrollOffset,
                    size = itemSize,
                ),
            ),
            viewportStartOffset = viewportStartOffset,
            viewportEndOffset = viewportEndOffset,
        )

        assertNotNull(restored)
        assertEquals(offsetWithinBlock, restored.position.offsetWithinBlock)
    }

    protected fun section(owner: String, texts: List<String>): BookDocumentSection<String> {
        var offset = 0
        val blocks = texts.mapIndexed { index, text ->
            if (index > 0) offset += 2
            val start = offset
            offset += text.length
            BookDocumentBlock(
                id = BookDocumentBlockId("block-$index"),
                role = BookDocumentBlockRole(BookDocumentBlockKind.PARAGRAPH),
                content = BookDocumentBlockContent.Text(
                    BookDocumentRichText(
                        text = text,
                        range = BookDocumentTextRange(0, text.length),
                    ),
                ),
                plainText = text,
                sourceFragments = emptyList(),
                logicalStart = start,
                logicalEndExclusive = offset,
            )
        }
        val document = BookDocument(
            resourceId = "shared-resource",
            revision = "r1",
            content = BookDocumentContent(
                text = texts.joinToString("\n\n"),
                blocks = blocks,
                anchors = emptyMap(),
            ),
        )
        val prepared = PreparedBookDocument(
            document = document,
            blocks = blocks.map { PreparedBookDocumentBlock(it, SpannableString(it.plainText)) },
            combinedText = SpannableString(texts.joinToString("\n\n")),
        )
        return BookDocumentSection(
            key = owner,
            owner = owner,
            document = prepared,
            initialPosition = BookDocumentPosition(blocks.first().id, 0),
            resourceLoader = null,
        )
    }
}
