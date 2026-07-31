package mihon.book.api.document

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Typed link range relative to an owning [BookDocumentRichText].
 *
 * @property start inclusive UTF-16 offset.
 * @property endExclusive exclusive UTF-16 offset.
 * @property target validated semantic destination.
 */
@Serializable
data class BookDocumentLink(
    val start: Int,
    val endExclusive: Int,
    val target: BookDocumentLinkTarget,
) {
    init {
        require(start >= 0) { "document link start must not be negative" }
        require(endExclusive > start) { "document link range must not be empty" }
    }

    internal fun shifted(offset: Int): BookDocumentLink =
        copy(start = start + offset, endExclusive = endExclusive + offset)
}

/** Semantic destination of a document link. */
@Serializable
sealed interface BookDocumentLinkTarget {

    /**
     * Anchor within the current semantic document.
     *
     * @property fragment non-blank anchor fragment without `#`.
     */
    @Serializable
    @SerialName("anchor")
    data class Anchor(val fragment: String) : BookDocumentLinkTarget {
        init {
            require(fragment.isNotBlank()) { "document link anchor must not be blank" }
            require(!fragment.startsWith("#")) {
                "document link anchor must not include the fragment marker"
            }
        }
    }

    /**
     * External HTTP(S) destination handed off to the host.
     *
     * @property url absolute HTTP(S) URL.
     */
    @Serializable
    @SerialName("external")
    data class External(val url: String) : BookDocumentLinkTarget {
        init {
            require(
                url.startsWith("https://", ignoreCase = true) ||
                    url.startsWith("http://", ignoreCase = true),
            ) {
                "document external link must use HTTP or HTTPS"
            }
        }
    }
}

/**
 * Converts a sanitized href to a typed semantic target.
 *
 * @return a supported target, or `null` for unsupported schemes and empty fragments.
 */
fun String.toBookDocumentLinkTarget(): BookDocumentLinkTarget? = when {
    startsWith("#") && length > 1 -> BookDocumentLinkTarget.Anchor(removePrefix("#"))
    startsWith("https://", ignoreCase = true) || startsWith("http://", ignoreCase = true) ->
        BookDocumentLinkTarget.External(this)
    else -> null
}
