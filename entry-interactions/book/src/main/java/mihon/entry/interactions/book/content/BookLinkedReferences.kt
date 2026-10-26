package mihon.entry.interactions.book.content

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.Base64

/** What a document links to by reference: an image source, or a font's `src` in an `@font-face` rule. */
internal enum class BookLinkedResourceKind(
    /** Persisted in download manifests: changing it hides every resource of this kind downloaded before. */
    val idPrefix: String,
) {
    IMAGE("linked-image:"),
    FONT("linked-font:"),
    ;

    fun acceptsDataMediaType(mediaType: String): Boolean = when (this) {
        IMAGE -> mediaType.startsWith("image/")
        // Web pages embed fonts under every type browsers have accepted over the years.
        FONT -> mediaType.startsWith("font/") || mediaType.startsWith("application/font-") ||
            mediaType.startsWith("application/x-font-") || mediaType == "application/vnd.ms-opentype" ||
            mediaType == "application/octet-stream"
    }
}

/**
 * What a reference written in a document names, before any catalog lookup.
 *
 * Online and downloaded sessions classify the same raw references, and both derive linked resource IDs
 * with [linkedResourceId], so a downloaded chapter finds the resources stored for it.
 */
internal sealed interface BookLinkedReference {
    /** Nothing Katari can load: empty, script, or a malformed or mistyped `data:` URI. */
    data object Unusable : BookLinkedReference

    /** A name to look up as written: a catalog ID, or an unknown one that renders as unavailable. */
    data class Named(val resourceId: String) : BookLinkedReference

    data class Remote(val url: HttpUrl) : BookLinkedReference

    class Data(val bytes: ByteArray) : BookLinkedReference
}

internal fun classifyBookLinkedReference(
    reference: String,
    baseUrl: HttpUrl?,
    kind: BookLinkedResourceKind,
): BookLinkedReference {
    val source = reference.trim()
    return when {
        source.isEmpty() || source.startsWith("javascript:", ignoreCase = true) -> BookLinkedReference.Unusable
        source.startsWith("data:", ignoreCase = true) ->
            source.decodeDataUri(kind)?.let(BookLinkedReference::Data) ?: BookLinkedReference.Unusable
        else -> (source.toHttpUrlOrNull() ?: baseUrl?.resolve(source))
            ?.let { url -> BookLinkedReference.Remote(url.newBuilder().fragment(null).build()) }
            ?: BookLinkedReference.Named(source)
    }
}

/**
 * Persisted in download manifests: changing it hides every linked resource of chapters downloaded before.
 * It hashes the reference as written, since the base it resolves against is fixed for one publication.
 */
internal fun linkedResourceId(reference: String, kind: BookLinkedResourceKind): String = kind.idPrefix +
    MessageDigest.getInstance("SHA-256")
        .digest(reference.trim().encodeToByteArray())
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

private fun String.decodeDataUri(kind: BookLinkedResourceKind): ByteArray? {
    val header = substringBefore(',', missingDelimiterValue = "").substring("data:".length)
    if (header.isEmpty()) return null
    val parameters = header.split(';').map(String::trim)
    if (!kind.acceptsDataMediaType(parameters.first().lowercase())) return null
    val payload = substringAfter(',')
    val bytes = if (parameters.drop(1).any { it.equals("base64", ignoreCase = true) }) {
        try {
            Base64.getMimeDecoder().decode(payload)
        } catch (_: IllegalArgumentException) {
            return null
        }
    } else {
        payload.percentDecoded() ?: return null
    }
    return bytes.takeIf(ByteArray::isNotEmpty)
}

// Not URLDecoder: data URIs keep '+' as written, while form decoding turns it into a space.
private fun String.percentDecoded(): ByteArray? {
    val output = ByteArrayOutputStream(length)
    var index = 0
    while (index < length) {
        val char = this[index]
        if (char == '%') {
            val byte = substring(index + 1, minOf(index + 3, length)).takeIf { it.length == 2 }?.toIntOrNull(16)
                ?: return null
            output.write(byte)
            index += 3
        } else {
            val end = if (char.isHighSurrogate() && index + 1 < length) index + 2 else index + 1
            output.write(substring(index, end).encodeToByteArray())
            index = end
        }
    }
    return output.toByteArray()
}
