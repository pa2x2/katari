package mihon.entry.interactions.book.download

import kotlinx.coroutines.CancellationException
import mihon.book.api.BookContentResource
import mihon.book.api.BookContentResourcePage
import mihon.book.api.BookResourceAvailability
import mihon.book.api.BookResourceCacheState
import mihon.book.api.BookResourceCapability
import mihon.entry.interactions.book.BookByteRange
import mihon.entry.interactions.book.BookContentSession
import mihon.entry.interactions.book.BookMaterializationKey
import mihon.entry.interactions.book.BookMaterializationStore
import mihon.entry.interactions.book.MaterializedBookResource
import mihon.entry.interactions.book.OpenedBookResource
import java.io.FilterInputStream
import java.io.InputStream

/** Processor-facing session backed entirely by a verified durable download package. */
internal class DownloadedBookContentSession(
    private val download: VerifiedBookDownloadPackage,
    private val materializationStore: BookMaterializationStore,
) : BookContentSession {
    private val lock = Any()
    private val activeLeases = LinkedHashSet<AutoCloseable>()
    private var closed = false

    private val manifest = download.manifest
    private val metadataById = manifest.resources.associate { resource ->
        resource.id to BookContentResource(
            id = resource.id,
            title = resource.title,
            order = resource.order,
            groupId = resource.groupId,
            mediaType = resource.mediaType,
            size = resource.storedSize,
            revision = resource.revision,
            availability = BookResourceAvailability.AVAILABLE,
            cacheState = BookResourceCacheState.CACHED,
            capabilities = RESOURCE_CAPABILITIES,
        )
    }

    override val descriptor = manifest.descriptor
    override val publicationId = manifest.publicationId
    override val revision = manifest.publicationRevision
    override val catalogRevision = manifest.catalogRevision
    override val catalogCoverage = manifest.catalogCoverage
    override val resourceHierarchy = manifest.resourceHierarchy
    override val primaryResourceIds = manifest.primaryResourceIds

    override suspend fun listResources(cursor: String?, limit: Int): Result<BookContentResourcePage> = resultOf {
        checkOpen()
        require(limit in 1..MAX_RESOURCE_PAGE_SIZE) {
            "resource page limit must be between 1 and $MAX_RESOURCE_PAGE_SIZE"
        }
        val offset = parseCursor(cursor)
        require(offset in 0..manifest.resources.size) { "resource cursor is outside the downloaded package" }
        val resources = manifest.resources.drop(offset).take(limit).map { metadataById.getValue(it.id) }
        BookContentResourcePage(
            resources = resources,
            nextCursor = (offset + resources.size)
                .takeIf { it < manifest.resources.size }
                ?.let { CURSOR_PREFIX + it },
        )
    }

    override suspend fun getResource(resourceId: String): Result<BookContentResource> = resultOf {
        checkOpen()
        metadata(resourceId)
    }

    override suspend fun openResource(
        resourceId: String,
        range: BookByteRange?,
    ): Result<OpenedBookResource> = resultOf {
        checkOpen()
        val metadata = metadata(resourceId)
        validateRange(metadata.size, range)
        val stream = download.resources.getValue(resourceId).openInputStream()
        val sliced = try {
            if (range == null) stream else stream.slice(range)
        } catch (error: Throwable) {
            stream.close()
            throw error
        }
        DownloadedOpenedBookResource(metadata, sliced, ::unregisterLease).also(::registerLease)
    }

    override suspend fun materializeResource(resourceId: String): Result<MaterializedBookResource> = resultOf {
        checkOpen()
        val metadata = metadata(resourceId)
        val downloadedResource = manifest.resources.first { it.id == resourceId }
        materializationStore.acquire(
            key = BookMaterializationKey(
                publicationId = manifest.publicationId,
                resourceId = resourceId,
                revision = downloadedResource.sha256,
                mediaType = metadata.mediaType,
            ),
            metadata = metadata,
        ) { target ->
            download.resources.getValue(resourceId).openInputStream().use { input ->
                target.outputStream().buffered().use { output -> input.copyTo(output) }
            }
        }
    }.map { lease ->
        DownloadedMaterializedBookResource(lease, ::unregisterLease).also(::registerLease)
    }

    override fun close() {
        val leases = synchronized(lock) {
            if (closed) return
            closed = true
            activeLeases.toList().asReversed().also { activeLeases.clear() }
        }
        var failure: Throwable? = null
        leases.forEach { lease ->
            try {
                lease.close()
            } catch (error: Throwable) {
                if (failure == null) failure = error else failure?.addSuppressed(error)
            }
        }
        failure?.let { throw it }
    }

    private fun metadata(resourceId: String): BookContentResource =
        metadataById[resourceId] ?: throw NoSuchElementException("Unknown downloaded BOOK resource: $resourceId")

    private fun checkOpen() {
        synchronized(lock) { check(!closed) { "Downloaded BOOK content session is closed" } }
    }

    private fun registerLease(lease: AutoCloseable) {
        val registered = synchronized(lock) { if (closed) false else activeLeases.add(lease) }
        if (!registered) {
            lease.close()
            checkOpen()
        }
    }

    private fun unregisterLease(lease: AutoCloseable) {
        synchronized(lock) { activeLeases.remove(lease) }
    }

    private fun parseCursor(cursor: String?): Int {
        if (cursor == null) return 0
        require(cursor.startsWith(CURSOR_PREFIX)) { "invalid downloaded BOOK resource cursor" }
        return cursor.removePrefix(CURSOR_PREFIX).toIntOrNull()
            ?: throw IllegalArgumentException("invalid downloaded BOOK resource cursor")
    }

    private companion object {
        const val CURSOR_PREFIX = "offset:"
        const val MAX_RESOURCE_PAGE_SIZE = 500
        val RESOURCE_CAPABILITIES = setOf(
            BookResourceCapability.STREAM,
            BookResourceCapability.RANGE,
            BookResourceCapability.MATERIALIZE,
        )
    }
}

private class DownloadedOpenedBookResource(
    override val metadata: BookContentResource,
    override val stream: InputStream,
    private val onClose: (AutoCloseable) -> Unit,
) : OpenedBookResource {
    private var closed = false

    override fun close() {
        synchronized(this) {
            if (closed) return
            closed = true
        }
        try {
            stream.close()
        } finally {
            onClose(this)
        }
    }
}

private class DownloadedMaterializedBookResource(
    private val delegate: MaterializedBookResource,
    private val onClose: (AutoCloseable) -> Unit,
) : MaterializedBookResource {
    override val metadata: BookContentResource
        get() = delegate.metadata
    override val file
        get() = delegate.file
    private var closed = false

    override fun invalidate() = delegate.invalidate()

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

private fun validateRange(size: Long?, range: BookByteRange?) {
    if (range == null || size == null) return
    require(range.startInclusive <= size) { "range starts beyond the downloaded BOOK resource" }
    require(range.endExclusive == null || range.endExclusive <= size) {
        "range ends beyond the downloaded BOOK resource"
    }
}

private fun InputStream.slice(range: BookByteRange): InputStream {
    skipFully(range.startInclusive)
    return range.endExclusive
        ?.minus(range.startInclusive)
        ?.let { LimitedInputStream(this, it) }
        ?: this
}

private fun InputStream.skipFully(byteCount: Long) {
    var remaining = byteCount
    while (remaining > 0L) {
        val skipped = skip(remaining)
        if (skipped > 0L) {
            remaining -= skipped
        } else if (read() >= 0) {
            remaining--
        } else {
            throw IllegalArgumentException("range starts beyond the downloaded BOOK resource")
        }
    }
}

private class LimitedInputStream(
    delegate: InputStream,
    private var remaining: Long,
) : FilterInputStream(delegate) {
    override fun read(): Int {
        if (remaining == 0L) return -1
        return super.read().also { if (it >= 0) remaining-- }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (remaining == 0L) return -1
        val read = super.read(buffer, offset, minOf(length.toLong(), remaining).toInt())
        if (read > 0) remaining -= read
        return read
    }
}

private suspend inline fun <T> resultOf(crossinline block: suspend () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }
}
