package mihon.entry.interactions.book.prose

import android.text.SpannableStringBuilder
import mihon.entry.interactions.book.document.model.BookDocumentBlockId
import mihon.entry.interactions.book.document.model.BookDocumentBlockRole
import mihon.entry.interactions.book.document.model.BookDocumentListMarkerStyle
import mihon.entry.interactions.book.document.model.BookDocumentTableCellScope
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.security.MessageDigest

internal fun String.toArgbLong(): Long? {
    if (!matches(Regex("""#[0-9a-fA-F]{8}"""))) return null
    return drop(1).toLongOrNull(16)
}

internal fun Element.fragments(): List<String> =
    (listOf(this) + select("[id], a[name]"))
        .flatMap { element -> listOf(element.id(), element.attr("name")) }
        .filter(String::isNotBlank)
        .distinct()

internal fun Element.ownFragments(): List<String> =
    listOf(id(), attr("name")).filter(String::isNotBlank).distinct()

internal fun Element.positiveDimension(attribute: String): Int? =
    attr(attribute).toIntOrNull()?.takeIf { it in 1..MAX_IMAGE_DIMENSION }

internal fun Element.listMarkerStyle(): BookDocumentListMarkerStyle {
    if (tagName() != "ol") return BookDocumentListMarkerStyle.BULLET
    return when (attr("type")) {
        "a" -> BookDocumentListMarkerStyle.LOWER_ALPHA
        "A" -> BookDocumentListMarkerStyle.UPPER_ALPHA
        "i" -> BookDocumentListMarkerStyle.LOWER_ROMAN
        "I" -> BookDocumentListMarkerStyle.UPPER_ROMAN
        else -> BookDocumentListMarkerStyle.DECIMAL
    }
}

internal fun BookDocumentListMarkerStyle.marker(value: Int): String = when (this) {
    BookDocumentListMarkerStyle.DECIMAL -> "$value."
    BookDocumentListMarkerStyle.LOWER_ALPHA -> "${value.toAlphabetic().lowercase()}."
    BookDocumentListMarkerStyle.UPPER_ALPHA -> "${value.toAlphabetic()}."
    BookDocumentListMarkerStyle.LOWER_ROMAN -> "${value.toRoman().lowercase()}."
    BookDocumentListMarkerStyle.UPPER_ROMAN -> "${value.toRoman()}."
    BookDocumentListMarkerStyle.BULLET -> "•"
}

internal fun Int.toAlphabetic(): String {
    if (this <= 0) return toString()
    var remaining = this
    return buildString {
        while (remaining > 0) {
            remaining--
            insert(0, ('A'.code + remaining % 26).toChar())
            remaining /= 26
        }
    }
}

internal fun Int.toRoman(): String {
    if (this !in 1..3_999) return toString()
    var remaining = this
    return buildString {
        ROMAN_NUMERALS.forEach { (number, numeral) ->
            while (remaining >= number) {
                append(numeral)
                remaining -= number
            }
        }
    }
}

internal fun String.toTableScope(): BookDocumentTableCellScope? = when (lowercase()) {
    "row" -> BookDocumentTableCellScope.ROW
    "col" -> BookDocumentTableCellScope.COLUMN
    "rowgroup" -> BookDocumentTableCellScope.ROW_GROUP
    "colgroup" -> BookDocumentTableCellScope.COLUMN_GROUP
    else -> null
}

internal fun Node.hasReadableText(): Boolean = when (this) {
    is TextNode -> text().any(Char::isReadableDocumentCharacter)
    is Element -> text().any(Char::isReadableDocumentCharacter)
    else -> false
}

internal fun Char.isReadableDocumentCharacter(): Boolean =
    !isWhitespace() && this != '\u00A0' && this != '\u200B' && this != '\uFFFC'

internal fun Element.isBlockElement(): Boolean =
    tagName() in BLOCK_TAGS || hasAttr("data-katari-unsupported")

internal fun String.withParagraphTerminator(): String = trimEnd() + "\n\n"

internal fun normalizeParagraphBreaks(parsed: SpannableStringBuilder) {
    var index = parsed.length - 1
    while (index >= 0) {
        if (parsed[index] == '\n') {
            val end = index + 1
            while (index >= 0 && parsed[index] == '\n') index--
            val start = index + 1
            if (end - start >= 2) parsed.replace(start, end, "\n\n")
        } else {
            index--
        }
    }
}

internal fun uniqueBlockId(
    explicitId: String?,
    role: BookDocumentBlockRole,
    plainText: String,
    usedIds: MutableMap<String, Int>,
): BookDocumentBlockId {
    val base = explicitId ?: buildString {
        append("auto:")
        append(role.kind.name.lowercase())
        append(':')
        append(
            sha256(
                "${role.kind.name}:${role.level?.toString().orEmpty()}:${role.depth}:" +
                    "${role.ordered?.toString().orEmpty()}\u0000$plainText",
            ).take(BLOCK_HASH_LENGTH),
        )
    }
    val occurrence = usedIds.getOrDefault(base, 0)
    usedIds[base] = occurrence + 1
    return BookDocumentBlockId(if (occurrence == 0) base else "$base:$occurrence")
}

internal fun sha256(value: String): String {
    return MessageDigest.getInstance("SHA-256")
        .digest(value.encodeToByteArray())
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
}

internal val HEADING_TAGS = setOf("h1", "h2", "h3", "h4", "h5", "h6")
internal val DOCUMENT_STYLE_ATTRIBUTES = setOf(
    "data-katari-align",
    "data-katari-background",
    "data-katari-bold",
    "data-katari-border",
    "data-katari-color",
    "data-katari-font-generic",
    "data-katari-font-resource",
    "data-katari-font-scale",
    "data-katari-padding-em",
    "data-katari-white-space",
)
internal val CONTAINER_TAGS = setOf("article", "aside", "body", "div", "dl", "section")
internal val BLOCK_TAGS = HEADING_TAGS + CONTAINER_TAGS + setOf(
    "blockquote",
    "caption",
    "dd",
    "details",
    "dt",
    "figcaption",
    "figure",
    "hr",
    "img",
    "ol",
    "p",
    "pre",
    "table",
    "ul",
)
internal val ROMAN_NUMERALS = listOf(
    1_000 to "M",
    900 to "CM",
    500 to "D",
    400 to "CD",
    100 to "C",
    90 to "XC",
    50 to "L",
    40 to "XL",
    10 to "X",
    9 to "IX",
    5 to "V",
    4 to "IV",
    1 to "I",
)
internal const val OBJECT_REPLACEMENT_TEXT = "\uFFFC\n\n"
internal const val IMAGE_UNAVAILABLE_TEXT = "Image unavailable"
internal const val DISCLOSURE_SUMMARY_FALLBACK = "Additional content"
internal const val STRUCTURED_HTML_MAX_DIAGNOSTIC_LENGTH = 64
internal const val MAX_LIST_DEPTH = 8
internal const val MAX_TABLE_ROWS = 200
internal const val MAX_TABLE_COLUMNS = 24
internal const val MAX_TABLE_CELLS_PER_ROW = 24
internal const val MAX_IMAGE_DIMENSION = 32_768
internal const val BLOCK_HASH_LENGTH = 16
internal const val ANCHOR_MARKER_START = '\uE000'
internal const val ANCHOR_MARKER_END = '\uE001'
internal const val INLINE_STYLE_MARKER_START = '\uE002'
internal const val INLINE_STYLE_MARKER_END = '\uE003'
