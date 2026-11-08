package app.katari.book.api.document

import app.katari.book.api.model.BookPublicationModel
import app.katari.book.api.model.BookPublicationModelDescriptor
import kotlinx.serialization.Serializable

/** Prepared reflowable publication represented by canonical semantic documents. */
@Serializable
data class BookDocumentPublicationModel(
    val documents: List<BookDocument>,
) : BookPublicationModel {
    init {
        require(documents.isNotEmpty()) { "document publication must contain at least one document" }
        require(documents.distinctBy(BookDocument::resourceId).size == documents.size) {
            "document publication resource ids must be unique"
        }
    }

    override val descriptor: BookPublicationModelDescriptor = DESCRIPTOR

    fun document(resourceId: String): BookDocument? = documents.firstOrNull { it.resourceId == resourceId }

    companion object {
        val DESCRIPTOR = BookPublicationModelDescriptor(
            id = "book.document",
            version = 2,
        )
    }
}
