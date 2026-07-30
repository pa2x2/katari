package mihon.entry.interactions.book.document.model

internal data class BookDocumentLink(
    val start: Int,
    val endExclusive: Int,
    val target: BookDocumentLinkTarget,
) {
    init {
        require(start >= 0) { "document link start must not be negative" }
        require(endExclusive > start) { "document link range must not be empty" }
    }
}

internal fun List<BookDocumentLink>.fitInside(text: String): Boolean =
    all { it.start in text.indices && it.endExclusive in 1..text.length }

internal sealed interface BookDocumentLinkTarget {
    data class Anchor(val fragment: String) : BookDocumentLinkTarget {
        init {
            require(fragment.isNotBlank()) { "document link anchor must not be blank" }
        }
    }

    data class External(val url: String) : BookDocumentLinkTarget {
        init {
            require(url.isNotBlank()) { "document external link must not be blank" }
        }
    }
}

internal fun String.toBookDocumentLinkTarget(): BookDocumentLinkTarget? = when {
    startsWith("#") && length > 1 -> BookDocumentLinkTarget.Anchor(removePrefix("#"))
    startsWith("https://", ignoreCase = true) || startsWith("http://", ignoreCase = true) ->
        BookDocumentLinkTarget.External(this)
    else -> null
}
