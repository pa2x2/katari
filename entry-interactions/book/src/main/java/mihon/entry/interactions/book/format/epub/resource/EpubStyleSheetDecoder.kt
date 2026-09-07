package mihon.entry.interactions.book.format.epub.resource

import java.nio.charset.Charset

/** CSS byte decoding for packaged stylesheets: BOM, leading charset declaration, then UTF-8. */
internal fun ByteArray.decodeEpubStyleSheet(): String {
    val bom = STYLE_SHEET_BYTE_ORDER_MARKS.firstOrNull { (_, marker) ->
        size >= marker.size && marker.indices.all { this[it] == marker[it] }
    }
    val declaration = STYLE_SHEET_CHARSET.find(String(this, 0, minOf(size, 1_024), Charsets.ISO_8859_1))
    val declaredCharset = declaration?.groupValues?.get(1)?.let { label ->
        runCatching { Charset.forName(label) }.getOrNull()
    }?.let { charset ->
        // CSS treats an ASCII @charset declaration of UTF-16 as UTF-8; only a BOM selects UTF-16.
        if (charset.name().startsWith("UTF-16", true)) Charsets.UTF_8 else charset
    }
    val charset = bom?.first ?: declaredCharset ?: Charsets.UTF_8
    val start = bom?.second?.size ?: 0
    val decoded = String(this, start, size - start, charset)
    // Encoding declarations are metadata, not part of the first supported style rule's selector.
    return STYLE_SHEET_CHARSET.replaceFirst(decoded, "")
}

private val STYLE_SHEET_CHARSET = Regex("""^@charset "([^"\r\n]+)";""")
private val STYLE_SHEET_BYTE_ORDER_MARKS = listOf(
    Charsets.UTF_8 to byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()),
    Charsets.UTF_16BE to byteArrayOf(0xFE.toByte(), 0xFF.toByte()),
    Charsets.UTF_16LE to byteArrayOf(0xFF.toByte(), 0xFE.toByte()),
)
