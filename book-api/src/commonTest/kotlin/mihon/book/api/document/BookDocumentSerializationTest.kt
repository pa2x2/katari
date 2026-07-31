package mihon.book.api.document

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class BookDocumentSerializationTest {

    @Test
    fun `rich document survives serialization`() {
        val text = "• Rich item\n\nAlt\nCaption\n\nHeader\nCell\n\nSummary\nBody"
        val listText = "Rich item"
        val listStart = text.indexOf(listText)
        val listBlock = BookDocumentBlock(
            id = BookDocumentBlockId("list"),
            role = BookDocumentBlockRole(BookDocumentBlockKind.LIST, ordered = false),
            content = BookDocumentBlockContent.ListBlock(
                ordered = false,
                start = 1,
                markerStyle = BookDocumentListMarkerStyle.BULLET,
                items = listOf(
                    BookDocumentListItem(
                        content = BookDocumentRichText(
                            text = listText,
                            range = BookDocumentTextRange(listStart, listStart + listText.length),
                            links = listOf(
                                BookDocumentLink(
                                    start = 0,
                                    endExclusive = 4,
                                    target = BookDocumentLinkTarget.Anchor("rich"),
                                ),
                            ),
                            inlineStyles = listOf(
                                BookDocumentInlineStyleRange(
                                    start = 0,
                                    endExclusive = listText.length,
                                    style = BookDocumentInlineStyle(
                                        foregroundArgb = 0xFF112233,
                                        fontFamily = BookDocumentFontFamily.Resource("font"),
                                        bold = true,
                                        italic = true,
                                        underline = true,
                                        strikethrough = true,
                                        subscript = true,
                                        code = true,
                                        small = true,
                                    ),
                                ),
                            ),
                        ),
                        depth = 0,
                        marker = "•",
                    ),
                ),
            ),
            plainText = listText,
            sourceFragments = listOf("rich"),
            logicalStart = 0,
            logicalEndExclusive = text.indexOf("Alt") - 1,
        )
        val figureStart = text.indexOf("Alt")
        val captionStart = text.indexOf("Caption")
        val figureEnd = text.indexOf("Header") - 1
        val figureBlock = BookDocumentBlock(
            id = BookDocumentBlockId("figure"),
            role = BookDocumentBlockRole(BookDocumentBlockKind.FIGURE),
            content = BookDocumentBlockContent.Figure(
                image = BookDocumentImage(
                    resourceId = "image",
                    alternativeText = BookDocumentRichText(
                        text = "Alt",
                        range = BookDocumentTextRange(0, 3),
                    ),
                    width = 640,
                    height = 320,
                ),
                caption = BookDocumentRichText(
                    text = "Caption",
                    range = BookDocumentTextRange(
                        captionStart - figureStart,
                        captionStart - figureStart + "Caption".length,
                    ),
                ),
            ),
            plainText = "Alt Caption",
            sourceFragments = emptyList(),
            logicalStart = figureStart,
            logicalEndExclusive = figureEnd,
        )
        val tableStart = text.indexOf("Header")
        val cellStart = text.indexOf("Cell")
        val tableEnd = text.indexOf("Summary") - 1
        val tableBlock = BookDocumentBlock(
            id = BookDocumentBlockId("table"),
            role = BookDocumentBlockRole(BookDocumentBlockKind.TABLE),
            content = BookDocumentBlockContent.Table(
                caption = null,
                rows = listOf(
                    BookDocumentTableRow(
                        cells = listOf(
                            BookDocumentTableCell(
                                content = BookDocumentRichText(
                                    text = "Header",
                                    range = BookDocumentTextRange(0, "Header".length),
                                ),
                                header = true,
                                scope = BookDocumentTableCellScope.COLUMN,
                                columnSpan = 1,
                                rowSpan = 1,
                            ),
                        ),
                    ),
                    BookDocumentTableRow(
                        cells = listOf(
                            BookDocumentTableCell(
                                content = BookDocumentRichText(
                                    text = "Cell",
                                    range = BookDocumentTextRange(
                                        cellStart - tableStart,
                                        cellStart - tableStart + "Cell".length,
                                    ),
                                ),
                                header = false,
                                scope = null,
                                columnSpan = 1,
                                rowSpan = 1,
                            ),
                        ),
                    ),
                ),
                columnCount = 1,
            ),
            plainText = "Header Cell",
            sourceFragments = emptyList(),
            logicalStart = tableStart,
            logicalEndExclusive = tableEnd,
        )
        val bodyBlock = bookDocumentTextBlock("body", "Body", logicalStart = 0)
        val body = BookDocumentContent(
            text = "Body",
            blocks = listOf(bodyBlock),
            anchors = emptyMap(),
        )
        val disclosureStart = text.indexOf("Summary")
        val disclosureBlock = BookDocumentBlock(
            id = BookDocumentBlockId("disclosure"),
            role = BookDocumentBlockRole(BookDocumentBlockKind.DISCLOSURE),
            content = BookDocumentBlockContent.Disclosure(
                summary = BookDocumentRichText(
                    text = "Summary",
                    range = BookDocumentTextRange(0, "Summary".length),
                ),
                body = body,
                bodyStartWithinBlock = "Summary\n".length,
                initiallyExpanded = true,
            ),
            plainText = "Summary Body",
            sourceFragments = emptyList(),
            logicalStart = disclosureStart,
            logicalEndExclusive = text.length,
        )
        val document = bookDocument(
            text = text,
            blocks = listOf(listBlock, figureBlock, tableBlock, disclosureBlock),
            anchors = mapOf("rich" to BookDocumentPosition(listBlock.id, listStart)),
            resourceIds = setOf("image", "font"),
        )

        val encoded = Json.encodeToString(document)
        val restored = Json.decodeFromString<BookDocument>(encoded)

        assertEquals(document, restored)
        assertEquals(
            document.blocks.single {
                it.id == listBlock.id
            }.inlineStyles,
            restored.blocks.first().inlineStyles,
        )
    }

    @Test
    fun `every sealed block content kind has a stable serialization round trip`() {
        val bodyBlock = bookDocumentTextBlock("body", "Body", logicalStart = 0)
        val body = BookDocumentContent(
            text = "Body",
            blocks = listOf(bodyBlock),
            anchors = emptyMap(),
        )
        val text = BookDocumentRichText(
            text = "Text",
            range = BookDocumentTextRange(0, 4),
        )
        val contents = listOf<BookDocumentBlockContent>(
            BookDocumentBlockContent.Text(text),
            BookDocumentBlockContent.ListBlock(
                ordered = false,
                start = 1,
                markerStyle = BookDocumentListMarkerStyle.BULLET,
                items = listOf(BookDocumentListItem(text, depth = 0, marker = "•")),
            ),
            BookDocumentBlockContent.Figure(
                image = BookDocumentImage(
                    resourceId = "image",
                    alternativeText = text,
                    width = null,
                    height = null,
                ),
                caption = null,
            ),
            BookDocumentBlockContent.Table(
                caption = null,
                rows = listOf(
                    BookDocumentTableRow(
                        listOf(
                            BookDocumentTableCell(
                                content = text,
                                header = true,
                                scope = BookDocumentTableCellScope.COLUMN,
                                columnSpan = 1,
                                rowSpan = 1,
                            ),
                        ),
                    ),
                ),
                columnCount = 1,
            ),
            BookDocumentBlockContent.Disclosure(
                summary = text,
                body = body,
                bodyStartWithinBlock = text.range.endExclusive,
                initiallyExpanded = false,
            ),
            BookDocumentBlockContent.ThematicBreak,
            BookDocumentBlockContent.Unsupported("math"),
        )

        val encoded = Json.encodeToString(contents)
        val restored = Json.decodeFromString<List<BookDocumentBlockContent>>(encoded)

        assertEquals(contents, restored)
    }

    @Test
    fun `every sealed link and font kind has a stable serialization round trip`() {
        val linkTargets = listOf<BookDocumentLinkTarget>(
            BookDocumentLinkTarget.Anchor("section"),
            BookDocumentLinkTarget.External("https://example.invalid/section"),
        )
        val fontFamilies = listOf<BookDocumentFontFamily>(
            BookDocumentFontFamily.Generic(BookDocumentFontFamily.GenericFamily.SERIF),
            BookDocumentFontFamily.Generic(BookDocumentFontFamily.GenericFamily.SANS_SERIF),
            BookDocumentFontFamily.Generic(BookDocumentFontFamily.GenericFamily.MONOSPACE),
            BookDocumentFontFamily.Resource("font"),
        )

        assertEquals(
            linkTargets,
            Json.decodeFromString<List<BookDocumentLinkTarget>>(
                Json.encodeToString(linkTargets),
            ),
        )
        assertEquals(
            fontFamilies,
            Json.decodeFromString<List<BookDocumentFontFamily>>(
                Json.encodeToString(fontFamilies),
            ),
        )
    }
}
