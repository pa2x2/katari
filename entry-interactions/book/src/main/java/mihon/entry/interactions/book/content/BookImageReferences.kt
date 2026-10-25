package mihon.entry.interactions.book.content

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.Base64

/**
 * What an image source written in a document names, before any catalog lookup.
 *
 * Online and downloaded sessions classify the same raw sources, and both derive linked resource IDs
 * with [linkedImageResourceId], so a downloaded chapter finds the images stored for it.
 */
internal sealed interface BookImageReference {
    /** Nothing Katari can load: empty, script, or a malformed `data:` image. */
    data object Unusable : BookImageReference

    /** A name to look up as written: a catalog ID, or an unknown one that renders as unavailable. */
    data class Named(val resourceId: String) : BookImageReference

    data class Remote(val url: HttpUrl) : BookImageReference

    class Data(val bytes: ByteArray) : BookImageReference
}

internal fun classifyBookImageReference(reference: String, baseUrl: HttpUrl?): BookImageReference {
    val source = reference.trim()
    return when {
        source.isEmpty() || source.startsWith("javascript:", ignoreCase = true) -> BookImageReference.Unusable
        source.startsWith("data:", ignoreCase = true) ->
            source.decodeDataImage()?.let(BookImageReference::Data) ?: BookImageReference.Unusable
        else -> (source.toHttpUrlOrNull() ?: baseUrl?.resolve(source))
            ?.let { url -> BookImageReference.Remote(url.newBuilder().fragment(null).build()) }
            ?: BookImageReference.Named(source)
    }
}

/**
 * Persisted in download manifests: changing it hides every image of chapters downloaded before.
 * It hashes the source as written, since the base it resolves against is fixed for one publication.
 */
internal fun linkedImageResourceId(reference: String): String = LINKED_IMAGE_ID_PREFIX +
    MessageDigest.getInstance("SHA-256")
        .digest(reference.trim().encodeToByteArray())
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

private fun String.decodeDataImage(): ByteArray? {
    val header = substringBefore(',', missingDelimiterValue = "").substring("data:".length)
    if (header.isEmpty()) return null
    val parameters = header.split(';').map(String::trim)
    if (!parameters.first().startsWith("image/", ignoreCase = true)) return null
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

private const val LINKED_IMAGE_ID_PREFIX = "linked-image:"
