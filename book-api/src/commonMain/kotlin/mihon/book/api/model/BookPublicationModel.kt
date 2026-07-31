package mihon.book.api.model

import kotlinx.serialization.Serializable

/** Stable identity and compatibility metadata for one prepared publication-model family. */
@Serializable
data class BookPublicationModelDescriptor(
    val id: String,
    val version: Int = 1,
) {
    init {
        require(id.isNotBlank()) { "BOOK publication model id must not be blank" }
        require(version > 0) { "BOOK publication model version must be positive" }
    }
}

/** Shared contract for a publication model produced before a reader implementation is selected. */
interface BookPublicationModel {
    val descriptor: BookPublicationModelDescriptor
}
