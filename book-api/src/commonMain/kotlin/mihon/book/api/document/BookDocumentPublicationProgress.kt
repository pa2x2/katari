package mihon.book.api.document

/** Logical-text weighting for progress through an ordered semantic-document publication. */
class BookDocumentPublicationProgress(
    documents: List<BookDocument>,
) {
    private val extents = documents.associate { document ->
        document.resourceId to document.logicalExtent.coerceAtLeast(1).toLong()
    }
    private val precedingExtents = buildMap(documents.size) {
        var preceding = 0L
        documents.forEach { document ->
            put(document.resourceId, preceding)
            preceding += extents.getValue(document.resourceId)
        }
    }
    private val totalExtent = extents.values.sum().coerceAtLeast(1L)

    init {
        require(documents.isNotEmpty()) { "publication progress requires at least one document" }
    }

    /** Returns weighted publication progression for a position within [resourceId]. */
    fun totalProgression(resourceId: String, progression: Double): Double {
        val preceding = requireNotNull(precedingExtents[resourceId]) {
            "Unknown publication document resource: $resourceId"
        }
        val extent = extents.getValue(resourceId)
        return ((preceding + extent * progression.coerceIn(0.0, 1.0)) / totalExtent)
            .coerceIn(0.0, 1.0)
    }
}
