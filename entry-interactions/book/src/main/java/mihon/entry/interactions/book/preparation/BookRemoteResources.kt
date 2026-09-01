package mihon.entry.interactions.book.preparation

/** Passive network asset class declared by a prepared publication. */
internal enum class BookRemoteResourceType {
    IMAGE,
    FONT,
}

internal data class BookRemoteResourceRequest(
    val origin: String,
    val type: BookRemoteResourceType,
)

internal data class BookRemoteResourceReference(
    val resourceId: String,
    val url: String,
    val type: BookRemoteResourceType,
)

/** Authorization boundary exposed by a prepared publication with optional network assets. */
internal interface BookRemoteResourceAuthorization {
    val remoteResourceRequests: Set<BookRemoteResourceRequest>
    fun authorizeRemoteOrigins(origins: Set<String>)
}
