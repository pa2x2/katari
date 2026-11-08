package mihon.book.api.document

import kotlinx.serialization.Serializable

/** Domain-neutral language and direction context for a semantic inline range. */
@Serializable
data class BookDocumentTextContext(
    val languageTag: String? = null,
    val direction: BookDocumentTextDirection? = null,
) {
    init {
        require(languageTag == null || languageTag.isNotBlank()) {
            "document inline language tag must not be blank"
        }
    }

    internal val isEmpty: Boolean
        get() = languageTag == null && direction == null
}
