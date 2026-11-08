package mihon.book.api.document

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One top-level or nested semantic block mapped into canonical document text.
 *
 * @property id stable identity within the owning [BookDocumentContent].
 * @property role semantic role used by reader projections.
 * @property content structured content carried by this block.
 * @property plainText outer-whitespace-trimmed text for search and accessibility, never for range
 * arithmetic.
 * @property sourceFragments provider fragments associated with this block.
 * @property style validated block presentation hints.
 * @property logicalStart inclusive UTF-16 offset in the owning canonical text.
 * @property logicalEndExclusive exclusive UTF-16 offset in the owning canonical text.
 */
@Serializable
data class BookDocumentBlock(
    val id: BookDocumentBlockId,
    val role: BookDocumentBlockRole,
    val content: BookDocumentBlockContent,
    val plainText: String,
    val sourceFragments: List<String>,
    val style: BookDocumentStyle = BookDocumentStyle(),
    val logicalStart: Int,
    val logicalEndExclusive: Int,
) {
    init {
        require(plainText == plainText.trim()) {
            "block plain text must not contain outer whitespace"
        }
        require(sourceFragments.none(String::isBlank)) { "block source fragments must not be blank" }
        require(sourceFragments.distinct().size == sourceFragments.size) {
            "block source fragments must be unique"
        }
        require(logicalStart >= 0) { "block logical start must not be negative" }
        require(logicalEndExclusive > logicalStart) { "block logical range must not be empty" }
    }

    /** UTF-16 length of this block in canonical text. */
    val logicalLength: Int
        get() = logicalEndExclusive - logicalStart

    /** Block-relative links aggregated from all rich-text leaves in this block. */
    val links: List<BookDocumentLink>
        get() = buildList {
            content.directRichTextLeaves().forEach { richText ->
                addAll(richText.links.map { it.shifted(richText.range.start) })
            }
            content.nestedContents().forEach { (start, body) ->
                body.blocks.forEach { nestedBlock ->
                    addAll(nestedBlock.links.map { it.shifted(start + nestedBlock.logicalStart) })
                }
            }
        }

    /** Block-relative inline styles aggregated from all rich-text leaves in this block. */
    val inlineStyles: List<BookDocumentInlineStyleRange>
        get() = buildList {
            content.directRichTextLeaves().forEach { richText ->
                addAll(richText.inlineStyles.map { it.shifted(richText.range.start) })
            }
            content.nestedContents().forEach { (start, body) ->
                body.blocks.forEach { nestedBlock ->
                    addAll(nestedBlock.inlineStyles.map { it.shifted(start + nestedBlock.logicalStart) })
                }
            }
        }
}

/**
 * Semantic classification and hierarchy metadata for a block.
 *
 * @property kind primary semantic kind.
 * @property level optional positive heading or outline level.
 * @property depth zero-based nesting depth.
 * @property ordered optional list ordering hint.
 */
@Serializable
data class BookDocumentBlockRole(
    val kind: BookDocumentBlockKind,
    val level: Int? = null,
    val depth: Int = 0,
    val ordered: Boolean? = null,
) {
    init {
        require(level == null || level > 0) { "document block level must be positive" }
        require(depth >= 0) { "document block depth must not be negative" }
    }
}

/** Supported semantic block kinds. */
@Serializable
enum class BookDocumentBlockKind {
    PARAGRAPH,
    HEADING,
    LIST,
    LIST_ITEM,
    QUOTE,
    PREFORMATTED,
    TABLE,
    FIGURE,
    CAPTION,
    THEMATIC_BREAK,
    DISCLOSURE,
    SIDE_BY_SIDE,
    CALLOUT,
    NOTE,
    UNSUPPORTED,
    OTHER,
}

/** Structured semantic content carried by a [BookDocumentBlock]. */
@Serializable
sealed interface BookDocumentBlockContent {

    /**
     * Ordinary or preformatted rich text.
     *
     * @property value text and inline semantics mapped into the owning block.
     * @property preformatted whether whitespace must be preserved.
     */
    @Serializable
    @SerialName("text")
    data class Text(
        val value: BookDocumentRichText,
        val preformatted: Boolean = false,
    ) : BookDocumentBlockContent

    /**
     * Ordered or unordered list.
     *
     * @property ordered whether the list is ordinal.
     * @property start first ordinal value.
     * @property markerStyle semantic marker style.
     * @property items flattened items with explicit depth and marker metadata.
     */
    @Serializable
    @SerialName("list")
    data class ListBlock(
        val ordered: Boolean,
        val start: Int,
        val markerStyle: BookDocumentListMarkerStyle,
        val items: List<BookDocumentListItem>,
    ) : BookDocumentBlockContent {
        init {
            require(items.isNotEmpty()) { "document list must contain at least one item" }
        }
    }

    /**
     * Validated image and optional rich caption.
     *
     * @property image referenced image and alternative text.
     * @property caption optional rich caption mapped into the owning block.
     */
    @Serializable
    @SerialName("figure")
    data class Figure(
        val image: BookDocumentImage,
        val caption: BookDocumentRichText?,
    ) : BookDocumentBlockContent

    /**
     * Rowspan-aware semantic table.
     *
     * @property caption optional rich caption mapped into the owning block.
     * @property rows table rows in source order.
     * @property columnCount validated grid width.
     */
    @Serializable
    @SerialName("table")
    data class Table(
        val caption: BookDocumentRichText?,
        val rows: List<BookDocumentTableRow>,
        val columnCount: Int,
    ) : BookDocumentBlockContent {
        init {
            require(rows.isNotEmpty()) { "document table must contain at least one row" }
            require(columnCount in 1..MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN) {
                "document table must contain between 1 and $MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN columns"
            }
            require(rows.layoutBookDocumentTable()?.columnCount == columnCount) {
                "document table column count must match its rowspan-aware grid"
            }
        }
    }

    /**
     * Expandable summary and recursive semantic body.
     *
     * @property summary rich summary mapped into the owning disclosure block.
     * @property body canonical recursive body content.
     * @property bodyStartWithinBlock offset at which [body] begins in the owning block.
     * @property initiallyExpanded initial provider-declared expanded state.
     */
    @Serializable
    @SerialName("disclosure")
    data class Disclosure(
        val summary: BookDocumentRichText,
        val body: BookDocumentContent,
        val bodyStartWithinBlock: Int,
        val initiallyExpanded: Boolean,
    ) : BookDocumentBlockContent {
        init {
            require(summary.text.isNotBlank()) { "document disclosure summary must not be blank" }
            require(bodyStartWithinBlock >= summary.range.endExclusive) {
                "document disclosure body must not overlap its summary"
            }
        }
    }

    /**
     * Passages laid out next to each other, such as a status window's stat cards or a gallery, from a flex or grid
     * container. Readers that can't fit them side by side stack the items in order.
     *
     * @property layout how the items share the row.
     * @property items items in reading order; each body maps into the owning block like a disclosure body.
     */
    @Serializable
    @SerialName("side_by_side")
    data class SideBySide(
        val layout: BookDocumentSideBySideLayout,
        val items: List<BookDocumentSideBySideItem>,
    ) : BookDocumentBlockContent {
        init {
            require(items.size >= 2) { "document side-by-side content must contain at least two items" }
            require(
                items.zipWithNext().all { (first, second) ->
                    first.bodyEndWithinBlock <= second.bodyStartWithinBlock
                },
            ) {
                "document side-by-side items must be ordered and non-overlapping"
            }
        }
    }

    /** Semantic thematic break. */
    @Serializable
    @SerialName("thematic_break")
    data object ThematicBreak : BookDocumentBlockContent

    /**
     * Explicit readable placeholder for unsupported passive content.
     *
     * @property elementType bounded diagnostic element type.
     */
    @Serializable
    @SerialName("unsupported")
    data class Unsupported(
        val elementType: String,
    ) : BookDocumentBlockContent {
        init {
            require(elementType.isNotBlank()) { "unsupported element type must not be blank" }
            require(elementType.length <= MAX_DIAGNOSTIC_LENGTH) {
                "unsupported element type is too long"
            }
        }
    }
}

private const val MAX_DIAGNOSTIC_LENGTH = 64

internal fun BookDocumentBlockContent.directRichTextLeaves(): List<BookDocumentRichText> = when (this) {
    is BookDocumentBlockContent.Text -> listOf(value)
    is BookDocumentBlockContent.ListBlock -> items.map(BookDocumentListItem::content)
    is BookDocumentBlockContent.Figure -> listOfNotNull(image.alternativeText, caption)
    is BookDocumentBlockContent.Table -> listOfNotNull(caption) + rows.flatMap { row ->
        row.cells.map(BookDocumentTableCell::content)
    }
    is BookDocumentBlockContent.Disclosure -> listOf(summary)
    is BookDocumentBlockContent.SideBySide,
    BookDocumentBlockContent.ThematicBreak,
    is BookDocumentBlockContent.Unsupported,
    -> emptyList()
}

internal fun BookDocumentBlockContent.validateCanonicalText(blockText: String) {
    val leaves = directRichTextLeaves()
    require(
        leaves.zipWithNext().all { (first, second) ->
            first.range.endExclusive <= second.range.start
        },
    ) {
        "document rich-text leaves must be ordered and non-overlapping"
    }
    leaves.forEach { it.validateCanonicalText(blockText) }
    when (this) {
        is BookDocumentBlockContent.Text -> {
            require(value.range.start == 0 && value.range.endExclusive == blockText.length) {
                "document text content must cover its complete canonical block text"
            }
        }
        is BookDocumentBlockContent.Disclosure -> body.validateNestedIn(blockText, bodyStartWithinBlock)
        is BookDocumentBlockContent.SideBySide -> items.forEach { item ->
            item.body.validateNestedIn(blockText, item.bodyStartWithinBlock)
        }
        else -> Unit
    }
}

internal fun BookDocumentBlock.referencedResourceIds(): Set<String> = buildSet {
    (style.fontFamily as? BookDocumentFontFamily.Resource)?.resourceId?.let(::add)
    content.directRichTextLeaves().forEach { richText ->
        richText.inlineStyles.mapNotNullTo(this) { inline ->
            (inline.style.fontFamily as? BookDocumentFontFamily.Resource)?.resourceId
        }
    }
    when (val blockContent = content) {
        is BookDocumentBlockContent.Figure -> add(blockContent.image.resourceId)
        is BookDocumentBlockContent.Disclosure -> addAll(blockContent.body.resourceIds)
        is BookDocumentBlockContent.SideBySide -> blockContent.items.forEach { addAll(it.body.resourceIds) }
        else -> Unit
    }
}

private fun BookDocumentContent.validateNestedIn(blockText: String, start: Int) {
    require(start + text.length <= blockText.length) { "document nested content must fit inside its owning block" }
    require(blockText.regionMatches(thisOffset = start, other = text, otherOffset = 0, length = text.length)) {
        "document nested content must match canonical block text"
    }
}

/** Nested contents of [this] with their offsets in the owning block. */
internal fun BookDocumentBlockContent.nestedContents(): List<Pair<Int, BookDocumentContent>> = when (this) {
    is BookDocumentBlockContent.Disclosure -> listOf(bodyStartWithinBlock to body)
    is BookDocumentBlockContent.SideBySide -> items.map { it.bodyStartWithinBlock to it.body }
    else -> emptyList()
}
