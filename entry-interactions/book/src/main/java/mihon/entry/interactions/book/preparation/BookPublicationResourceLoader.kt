package mihon.entry.interactions.book.preparation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class BookPublicationResource(
    val resourceId: String,
    val mediaType: String?,
    val bytes: ByteArray,
)

/** Protected, publication-scoped resource access available to every prepared model renderer. */
internal interface BookPublicationResourceLoader {
    /** Invalidates composed failures when resource availability or authorization changes. */
    val generation: StateFlow<Int> get() = IMMUTABLE_RESOURCE_GENERATION

    suspend fun load(
        resourceId: String,
        acceptedMediaTypes: Set<String>,
        maxBytes: Int,
    ): Result<BookPublicationResource>
}

private val IMMUTABLE_RESOURCE_GENERATION = MutableStateFlow(0).asStateFlow()
