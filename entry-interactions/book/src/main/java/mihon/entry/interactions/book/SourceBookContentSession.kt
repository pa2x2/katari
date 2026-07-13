package mihon.entry.interactions.book

import eu.kanade.tachiyomi.source.entry.BookResourceHierarchyNode
import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import eu.kanade.tachiyomi.source.entry.BookSourceResource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import mihon.book.api.BookContentResource
import mihon.book.api.BookContentResourceGroup
import mihon.book.api.BookContentResourcePage
import mihon.book.api.BookResourceAvailability
import mihon.book.api.BookResourceCacheState
import mihon.book.api.BookResourceCapability
import tachiyomi.domain.entry.model.Entry
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * Katari-owned adapter between source BOOK media and format processors.
 *
 * Processors only receive this session and scoped resource handles. Source
 * instances, request headers, content URIs, and app references remain on this
 * side of the boundary.
 */
internal class SourceBookContentSession(
    private val source: UnifiedSource,
    entry: Entry,
    media: EntryMedia.Book,
    private val externalResolver: BookExternalResourceResolver,
    private val materializationStore: BookMaterializationStore,
) : BookContentSession {
    private val lock = Any()
    private val activeLeases = LinkedHashSet<AutoCloseable>()
    private var closed = false

    private val resources = buildResourceRecords(media)
    private val resourcesById = resources.associateBy(ResourceRecord::id)

    override val descriptor = media.descriptor
    override val publicationId = buildPublicationId(source.id, entry.url, media.publicationKeyOverride)
    override val revision = media.publicationRevision ?: UNVERSIONED_REVISION
    override val catalogRevision = media.catalog.revision
    override val catalogCoverage = media.catalog.coverage
    override val resourceHierarchy = media.hierarchy.map(BookResourceHierarchyNode::toProcessorGroup)
    override val primaryResourceIds = listOfNotNull(
        media.initialResourceId ?: media.catalog.resources.singleOrNull()?.id,
    )
    private val publicationRevision = media.publicationRevision

    init {
        entry.requireBook()
        require(entry.source == source.id) {
            "BOOK entry source ${entry.source} does not match session source ${source.id}"
        }
    }

    override suspend fun listResources(cursor: String?, limit: Int): Result<BookContentResourcePage> = resultOf {
        checkOpen()
        require(limit in 1..MAX_RESOURCE_PAGE_SIZE) {
            "resource page limit must be between 1 and $MAX_RESOURCE_PAGE_SIZE"
        }
        val offset = parseCursor(cursor)
        require(offset in 0..resources.size) { "resource cursor is outside the catalog" }
        val page = resources.drop(offset).take(limit).map { it.currentMetadata() }
        BookContentResourcePage(
            resources = page,
            nextCursor = (offset + page.size)
                .takeIf { it < resources.size }
                ?.let { "$CURSOR_PREFIX$it" },
        )
    }

    override suspend fun getResource(resourceId: String): Result<BookContentResource> = resultOf {
        checkOpen()
        resource(resourceId).currentMetadata()
    }

    override suspend fun openResource(
        resourceId: String,
        range: BookByteRange?,
    ): Result<OpenedBookResource> = resultOf {
        checkOpen()
        val record = resource(resourceId)
        record.requireAccessible()
        val metadata = record.currentMetadata()
        validateRange(metadata.size, range)

        val terminal = resolveTerminalLocation(
            resourceId = record.id,
            location = checkNotNull(record.location) { "BOOK resource $resourceId has no access location" },
            visitedSourceChildren = linkedSetOf(),
            depth = 0,
        )
        val raw = openTerminalLocation(terminal, range)
        val lease = SessionOpenedBookResource(
            metadata = metadata,
            stream = raw.stream,
            delegate = raw,
            onClose = ::unregisterLease,
        )
        registerLease(lease)
        lease
    }

    override suspend fun materializeResource(resourceId: String): Result<MaterializedBookResource> = resultOf {
        checkOpen()
        val record = resource(resourceId)
        val metadata = record.currentMetadata()
        metadata.size?.let { size ->
            require(size <= MAX_MATERIALIZED_BYTES) {
                "BOOK resource $resourceId exceeds the materialization limit"
            }
        }

        materializationStore.acquire(record.materializationKey(), metadata) { file ->
            val opened = openResource(resourceId).getOrThrow()
            try {
                copyToMaterialization(opened.stream, file)
            } finally {
                opened.close()
            }
        }
    }.map { cachedLease ->
        val sessionLease = SessionMaterializedBookResource(
            delegate = cachedLease,
            onClose = ::unregisterLease,
        )
        registerLease(sessionLease)
        sessionLease
    }

    override fun close() {
        val leases = synchronized(lock) {
            if (closed) return
            closed = true
            activeLeases.toList().asReversed().also { activeLeases.clear() }
        }

        var firstFailure: Throwable? = null
        leases.forEach { lease ->
            try {
                lease.close()
            } catch (error: Throwable) {
                if (firstFailure == null) firstFailure = error else firstFailure.addSuppressed(error)
            }
        }
        firstFailure?.let { throw it }
    }

    private fun resource(resourceId: String): ResourceRecord =
        resourcesById[resourceId] ?: throw NoSuchElementException("Unknown BOOK resource: $resourceId")

    private fun checkOpen() {
        synchronized(lock) { check(!closed) { "BOOK content session is closed" } }
    }

    private fun registerLease(lease: AutoCloseable) {
        val registered = synchronized(lock) {
            if (closed) false else activeLeases.add(lease)
        }
        if (!registered) {
            lease.close()
            checkOpen()
            error("BOOK resource lease was already registered")
        }
    }

    private fun unregisterLease(lease: AutoCloseable) {
        synchronized(lock) { activeLeases.remove(lease) }
    }

    private suspend fun resolveTerminalLocation(
        resourceId: String,
        location: BookResourceLocation,
        visitedSourceChildren: MutableSet<String>,
        depth: Int,
    ): BookResourceLocation {
        if (location !is BookResourceLocation.SourceChild) return location
        require(depth < MAX_SOURCE_CHILD_DEPTH) { "BOOK source-child resolution exceeded its depth limit" }
        require(location.resourceId == resourceId) {
            "BOOK source-child resource ${location.resourceId} does not match $resourceId"
        }
        check(visitedSourceChildren.add(location.sourceChildKey)) {
            "BOOK source-child resolution loop for ${location.sourceChildKey}"
        }

        val sourceChild = SEntryChapter.create().apply {
            url = location.sourceChildKey
            name = location.sourceChildKey
        }
        val nestedMedia = source.getMedia(sourceChild) as? EntryMedia.Book
            ?: error("BOOK source child ${location.sourceChildKey} returned non-BOOK media")
        val nestedResource = nestedMedia.catalog.resources.firstOrNull { it.id == resourceId }
        nestedResource?.requireAccessible()
        val nestedLocation = nestedMedia.initialResourceLocation
            ?.takeIf { nestedMedia.initialResourceId == resourceId }
            ?: nestedResource?.location
            ?: error("BOOK source child ${location.sourceChildKey} did not resolve resource $resourceId")

        return resolveTerminalLocation(
            resourceId = resourceId,
            location = nestedLocation,
            visitedSourceChildren = visitedSourceChildren,
            depth = depth + 1,
        )
    }

    private suspend fun openTerminalLocation(
        location: BookResourceLocation,
        range: BookByteRange?,
    ): ExternalBookResource {
        return when (location) {
            is BookResourceLocation.InlineBytes -> inlineResource(location.bytes, range)
            is BookResourceLocation.InlineText -> inlineResource(location.text.encodeToByteArray(), range)
            is BookResourceLocation.RemoteRequest,
            is BookResourceLocation.LocalUri,
            is BookResourceLocation.AppReference,
            -> externalResolver.open(location, range)
            is BookResourceLocation.SourceChild -> error("Nested source child was not fully resolved")
        }
    }

    private fun inlineResource(bytes: ByteArray, range: BookByteRange?): ExternalBookResource {
        val startLong = range?.startInclusive ?: 0L
        require(startLong <= bytes.size.toLong()) { "range starts beyond the inline BOOK resource" }
        val start = startLong.toInt()
        val end = range?.endExclusive?.coerceAtMost(bytes.size.toLong())?.toInt() ?: bytes.size
        val stream = ByteArrayInputStream(bytes, start, end - start)
        return SimpleExternalBookResource(stream)
    }

    private suspend fun copyToMaterialization(input: InputStream, output: java.io.File) = withContext(Dispatchers.IO) {
        output.outputStream().buffered().use { target ->
            val buffer = ByteArray(COPY_BUFFER_SIZE)
            var copied = 0L
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = input.read(buffer)
                if (read < 0) break
                copied += read
                require(copied <= MAX_MATERIALIZED_BYTES) { "BOOK resource exceeds the materialization limit" }
                target.write(buffer, 0, read)
            }
        }
    }

    private fun parseCursor(cursor: String?): Int {
        if (cursor == null) return 0
        require(cursor.startsWith(CURSOR_PREFIX)) { "invalid BOOK resource cursor" }
        return cursor.removePrefix(CURSOR_PREFIX).toIntOrNull()
            ?: throw IllegalArgumentException("invalid BOOK resource cursor")
    }

    private fun validateRange(size: Long?, range: BookByteRange?) {
        if (size == null || range == null) return
        require(range.startInclusive <= size) { "range starts beyond the BOOK resource" }
    }

    private suspend fun <T> resultOf(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private companion object {
        const val CURSOR_PREFIX = "offset:"
        const val UNVERSIONED_REVISION = "unversioned"
        const val COPY_BUFFER_SIZE = 32 * 1024
        const val MAX_RESOURCE_PAGE_SIZE = 500
        const val MAX_SOURCE_CHILD_DEPTH = 16
        const val MAX_MATERIALIZED_BYTES = 512L * 1024L * 1024L
    }

    private fun ResourceRecord.materializationKey(): BookMaterializationKey? {
        val stableRevision = metadata.revision ?: publicationRevision ?: return null
        return BookMaterializationKey(
            publicationId = publicationId,
            resourceId = id,
            revision = stableRevision,
            mediaType = metadata.mediaType,
        )
    }

    private fun ResourceRecord.currentMetadata(): BookContentResource {
        if (metadata.cacheState == BookResourceCacheState.CACHED) return metadata
        return metadata.copy(cacheState = materializationStore.cacheState(materializationKey()))
    }
}

/**
 * Katari-side resolver for locations which cannot be opened from inline source data.
 *
 * Implementations own authentication, network clients, content permissions,
 * app-reference lookup, and any durable cache. The returned stream must contain
 * exactly the requested [range] when one is supplied.
 */
internal interface BookExternalResourceResolver {
    /** Opens a remote request, local content URI, or app-owned reference. */
    suspend fun open(location: BookResourceLocation, range: BookByteRange?): ExternalBookResource
}

/** Scoped external stream. Closing it must cancel/release all underlying I/O. */
internal interface ExternalBookResource : AutoCloseable {
    val stream: InputStream
}

private class SimpleExternalBookResource(
    override val stream: InputStream,
) : ExternalBookResource {
    override fun close() = stream.close()
}

private class SessionOpenedBookResource(
    override val metadata: BookContentResource,
    override val stream: InputStream,
    private val delegate: ExternalBookResource,
    private val onClose: (AutoCloseable) -> Unit,
) : OpenedBookResource {
    private var closed = false

    override fun close() {
        synchronized(this) {
            if (closed) return
            closed = true
        }
        try {
            delegate.close()
        } finally {
            onClose(this)
        }
    }
}

private class SessionMaterializedBookResource(
    private val delegate: MaterializedBookResource,
    private val onClose: (AutoCloseable) -> Unit,
) : MaterializedBookResource {
    override val metadata: BookContentResource
        get() = delegate.metadata
    override val file: java.io.File
        get() = delegate.file
    private var closed = false

    override fun close() {
        synchronized(this) {
            if (closed) return
            closed = true
        }
        try {
            delegate.close()
        } finally {
            onClose(this)
        }
    }
}

private data class ResourceRecord(
    val id: String,
    val metadata: BookContentResource,
    val location: BookResourceLocation?,
)

private fun buildResourceRecords(media: EntryMedia.Book): List<ResourceRecord> {
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
        val accessible = resource.availability == BookResourceAvailability.UNKNOWN ||
            resource.availability == BookResourceAvailability.AVAILABLE
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
                availability = resource.availability,
                cacheState = if (location is BookResourceLocation.InlineBytes ||
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

private fun BookSourceResource.requireAccessible() {
    if (availability != BookResourceAvailability.UNKNOWN && availability != BookResourceAvailability.AVAILABLE) {
        throw BookResourceUnavailableException(id, availability)
    }
}

private fun ResourceRecord.requireAccessible() {
    if (metadata.availability != BookResourceAvailability.UNKNOWN &&
        metadata.availability != BookResourceAvailability.AVAILABLE
    ) {
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

private fun BookResourceHierarchyNode.toProcessorGroup(): BookContentResourceGroup = BookContentResourceGroup(
    id = id,
    title = title,
    resourceIds = resourceIds,
    children = children.map(BookResourceHierarchyNode::toProcessorGroup),
)

private fun buildPublicationId(sourceId: Long, entryUrl: String, override: String?): String = buildString {
    append("source:")
    append(sourceId)
    append(":entry:")
    append(entryUrl)
    if (override != null) {
        append(":publication:")
        append(override)
    }
}
