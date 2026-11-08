package mihon.book.api.document

internal fun bookDocumentTextBlock(
    id: String,
    text: String,
    logicalStart: Int,
    fragments: List<String> = emptyList(),
): BookDocumentBlock = BookDocumentBlock(
    id = BookDocumentBlockId(id),
    role = BookDocumentBlockRole(BookDocumentBlockKind.PARAGRAPH),
    content = BookDocumentBlockContent.Text(
        BookDocumentRichText(
            text = text,
            range = BookDocumentTextRange(0, text.length),
        ),
    ),
    plainText = text.trim(),
    sourceFragments = fragments,
    logicalStart = logicalStart,
    logicalEndExclusive = logicalStart + text.length,
)

internal fun bookDocument(
    text: String,
    blocks: List<BookDocumentBlock>,
    anchors: Map<String, BookDocumentPosition> = emptyMap(),
    resourceIds: Set<String> = emptySet(),
): BookDocument = BookDocument(
    resourceId = "chapter",
    revision = "r1",
    content = BookDocumentContent(
        text = text,
        blocks = blocks,
        anchors = anchors,
        resourceIds = resourceIds,
    ),
)
