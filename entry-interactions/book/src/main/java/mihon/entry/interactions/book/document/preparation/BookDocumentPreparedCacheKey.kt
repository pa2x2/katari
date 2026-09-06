package mihon.entry.interactions.book.document.preparation

import kotlinx.serialization.Serializable
import mihon.book.api.document.BookDocumentPublicationModel
import java.security.MessageDigest

@Serializable
internal data class BookDocumentPreparedCacheKey(
    val publicationId: String,
    val revision: String,
    val modelId: String = BookDocumentPublicationModel.DESCRIPTOR.id,
    val modelVersion: Int = BookDocumentPublicationModel.DESCRIPTOR.version,
) {
    init {
        require(publicationId.isNotBlank()) { "prepared document publication id must not be blank" }
        require(revision.isNotBlank()) { "prepared document revision must not be blank" }
    }

    fun diskKey(): String = MessageDigest.getInstance("SHA-256")
        .digest(listOf(publicationId, revision, modelId, modelVersion).joinToString("\u0000").encodeToByteArray())
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
}
