package eu.kanade.tachiyomi.source.lnreader.chapter

import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import eu.kanade.tachiyomi.source.entry.BookSourceResource
import java.util.Base64

/** A chapter's HTML with its inline pictures taken out into [images], which its `img` tags name by ID. */
internal class LiftedImages(val html: String, val images: List<BookSourceResource>)

/**
 * Takes the pictures a chapter carries as base64 `data:` URIs out of its HTML. Some plugins inline a volume's
 * illustrations, which takes a chapter to 17 M characters, four times what a prose chapter may hold; as resources of
 * their own, they don't count against that. A picture that doesn't decode stays inline.
 */
internal fun liftInlineImages(html: String): LiftedImages {
    val images = mutableListOf<BookSourceResource>()
    val lifted = INLINE_IMAGE.replace(html) { match ->
        val (attribute, quote, mediaType, data) = match.destructured
        val bytes = runCatching { Base64.getMimeDecoder().decode(data) }.getOrNull()
            ?.takeIf { it.size <= BookResourceLocation.MAX_INLINE_BYTES }
            ?: return@replace match.value
        val id = "image-${images.size + 1}"
        images += BookSourceResource(
            id = id,
            mediaType = mediaType.lowercase(),
            size = bytes.size.toLong(),
            location = BookResourceLocation.InlineBytes(bytes, mediaType.lowercase()),
        )
        "$attribute$quote$id$quote"
    }
    return LiftedImages(lifted, images)
}

private val INLINE_IMAGE = Regex(
    """(<img\b[^>]*?\bsrc\s*=\s*)(["'])data:(image/[a-z0-9.+-]+);base64,([^"']*)\2""",
    RegexOption.IGNORE_CASE,
)
