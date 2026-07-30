package mihon.entry.interactions.book.document.reader

import android.text.SpannableString
import mihon.entry.interactions.book.document.model.BookDocument
import mihon.entry.interactions.book.document.model.BookDocumentBlock
import mihon.entry.interactions.book.document.model.BookDocumentBlockId
import mihon.entry.interactions.book.document.model.BookDocumentBlockKind
import mihon.entry.interactions.book.document.model.BookDocumentBlockRole
import mihon.entry.interactions.book.document.model.BookDocumentPosition
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
                plainText = text,
                sourceFragments = emptyList(),
                logicalStart = start,
                logicalEndExclusive = offset,
            )
        }
        val document = BookDocument(
            resourceId = "shared-resource",
            revision = "r1",
            blocks = blocks,
            anchors = emptyMap(),
            logicalExtent = offset,
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
