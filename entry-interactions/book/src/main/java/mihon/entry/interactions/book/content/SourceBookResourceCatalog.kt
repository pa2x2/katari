package mihon.entry.interactions.book

import eu.kanade.tachiyomi.source.entry.BookResourceHierarchyNode
import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import eu.kanade.tachiyomi.source.entry.BookSourceResource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import mihon.book.api.BookContentResource
import mihon.book.api.BookContentResourceGroup
import mihon.book.api.BookResourceAvailability
import mihon.book.api.BookResourceCacheState
import mihon.book.api.BookResourceCapability

internal data class ResourceRecord(
    val id: String,
    val metadata: BookContentResource,
    val location: BookResourceLocation?,
)

internal fun buildResourceRecords(
    media: EntryMedia.Book,
    canResolveAppReferences: Boolean,
): List<ResourceRecord> {
    val byId = LinkedHashMap<String, BookSourceResource>()
    media.catalog.resources
        .withIndex()
        .sortedWith(
            compareBy<IndexedValue<BookSourceResource>> {
                it.value.order ?: Long.MAX_VALUE
            }.thenBy { it.index },
        )
        .map(IndexedValue<BookSourceResource>::value)
        .forEach { byId[it.id] = it }

    val initialResourceId = media.initialResourceId
    val initialLocation = media.initialResourceLocation
    if (initialResourceId != null) {
        val current = byId[initialResourceId]
        byId[initialResourceId] = current?.copy(location = initialLocation ?: current.location)
            ?: BookSourceResource(
                id = initialResourceId,
                mediaType = media.descriptor.format,
                revision = media.publicationRevision,
                availability = BookResourceAvailability.AVAILABLE,
                location = initialLocation,
            )
    }

    return byId.values.map { resource ->
        val location = resource.location
        val availability = when {
            location is BookResourceLocation.AppReference &&
                !canResolveAppReferences &&
                resource.availability.isAccessible() -> BookResourceAvailability.UNSUPPORTED_APP_ACCESS
            else -> resource.availability
        }
        val accessible = availability.isAccessible()
        ResourceRecord(
            id = resource.id,
            metadata = BookContentResource(
                id = resource.id,
                title = resource.title,
                order = resource.order,
                groupId = resource.groupId,
                mediaType = resource.mediaType ?: location.mediaType(),
                size = resource.size ?: location.inlineSize(),
                revision = resource.revision,
                availability = availability,
                cacheState = if (
                    location is BookResourceLocation.InlineBytes ||
                    location is BookResourceLocation.InlineText
                ) {
                    BookResourceCacheState.CACHED
                } else {
                    BookResourceCacheState.UNKNOWN
                },
                capabilities = if (location != null && accessible) {
                    setOf(
                        BookResourceCapability.STREAM,
                        BookResourceCapability.RANGE,
                        BookResourceCapability.MATERIALIZE,
                    )
                } else {
                    emptySet()
                },
            ),
            location = location,
        )
    }
}

internal fun BookResourceAvailability.isAccessible(): Boolean =
    this == BookResourceAvailability.UNKNOWN || this == BookResourceAvailability.AVAILABLE

internal fun BookSourceResource.requireAccessible() {
    if (!availability.isAccessible()) {
        throw BookResourceUnavailableException(id, availability)
    }
}

internal fun ResourceRecord.requireAccessible() {
    if (!metadata.availability.isAccessible()) {
        throw BookResourceUnavailableException(id, metadata.availability)
    }
}

internal class BookResourceUnavailableException(
    val resourceId: String,
    val availability: BookResourceAvailability,
) : IllegalStateException("BOOK resource $resourceId is unavailable: $availability")

private fun BookResourceLocation?.mediaType(): String? = when (this) {
    is BookResourceLocation.InlineBytes -> mediaType
    is BookResourceLocation.InlineText -> mediaType
    else -> null
}

private fun BookResourceLocation?.inlineSize(): Long? = when (this) {
    is BookResourceLocation.InlineBytes -> bytes.size.toLong()
    is BookResourceLocation.InlineText -> text.encodeToByteArray().size.toLong()
    else -> null
}

internal fun BookResourceHierarchyNode.toProcessorGroup(): BookContentResourceGroup = BookContentResourceGroup(
    id = id,
    title = title,
    resourceIds = resourceIds,
    children = children.map(BookResourceHierarchyNode::toProcessorGroup),
)

internal fun buildPublicationId(sourceId: Long, entryUrl: String, override: String?): String = buildString {
    append("source:")
    append(sourceId)
    append(":entry:")
    append(entryUrl)
    if (override != null) {
        append(":publication:")
        append(override)
    }
}
