package mihon.entry.interactions.book.content

import eu.kanade.tachiyomi.source.entry.BookResourceHierarchyNode
import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import mihon.book.api.BookContentResource
import mihon.book.api.BookContentResourcePage
import mihon.book.api.BookResourceAvailability
import mihon.book.api.BookResourceCacheState
import mihon.entry.interactions.book.runtime.requireBook
import tachiyomi.domain.entry.model.Entry
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Katari-owned adapter between source BOOK media and content preparers.
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

    private val resources = buildResourceRecords(
        media = media,
        canResolveAppReferences = externalResolver.canResolveAppReferences,
    )
    private val resourcesById = resources.associateBy(ResourceRecord::id)

    override val descriptor = media.descriptor
    override val publicationId = buildPublicationId(source.id, entry.url, media.publicationKeyOverride)
    override val revision = media.publicationRevision ?: UNVERSIONED_REVISION
    override val languages = listOfNotNull((source as? EntryCatalogueSource)?.lang)
        .normalizedBookContentLanguages()
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
            metadata = raw.mediaType?.let { metadata.copy(mediaType = it) } ?: metadata,
            stream = raw.stream,
            delegate = raw,
            onClose = ::unregisterLease,
        )
        registerLease(lease)
        lease
    }

    override suspend fun materializeResource(resourceId: String): Result<MaterializedBookResource> =
        materializeResource(resourceId, MAX_MATERIALIZED_BYTES)

    internal suspend fun materializeResource(
        resourceId: String,
        maxBytes: Long,
    ): Result<MaterializedBookResource> = resultOf {
        checkOpen()
        val boundedMaxBytes = maxBytes.coerceIn(1L, MAX_MATERIALIZED_BYTES)
        val record = resource(resourceId)
        val metadata = record.currentMetadata()
        metadata.size?.let { size ->
            if (size > boundedMaxBytes) {
                throw BookResourceMaterializationLimitException(
                    "BOOK resource $resourceId exceeds its $boundedMaxBytes-byte acquisition limit",
                )
            }
        }

        val cachedLease = materializationStore.acquire(record.materializationKey(), metadata) { file ->
            val opened = openResource(resourceId).getOrThrow()
            try {
                copyToMaterialization(opened.stream, file, boundedMaxBytes)
            } finally {
                opened.close()
            }
        }
        if (cachedLease.file.length() > boundedMaxBytes) {
            cachedLease.invalidate()
            cachedLease.close()
            throw BookResourceMaterializationLimitException(
                "BOOK resource $resourceId exceeds its $boundedMaxBytes-byte acquisition limit",
            )
        }
        cachedLease
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
        if (location !is BookResourceLocation.SourceChild) {
            if (location is BookResourceLocation.AppReference && !externalResolver.canResolveAppReferences) {
                throw BookResourceUnavailableException(
                    resourceId = resourceId,
                    availability = BookResourceAvailability.UNSUPPORTED_APP_ACCESS,
                )
            }
            return location
        }
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

    private suspend fun copyToMaterialization(
        input: InputStream,
        output: File,
        maxBytes: Long,
    ) = withContext(Dispatchers.IO) {
        output.outputStream().buffered().use { target ->
            val buffer = ByteArray(COPY_BUFFER_SIZE)
            var copied = 0L
            while (true) {
                currentCoroutineContext().ensureActive()
                val remainingWithOverflowByte = maxBytes - copied + 1L
                val read = input.read(
                    buffer,
                    0,
                    minOf(buffer.size.toLong(), remainingWithOverflowByte).toInt(),
                )
                if (read < 0) break
                copied += read
                if (copied > maxBytes) {
                    throw BookResourceMaterializationLimitException(
                        "BOOK resource exceeds its $maxBytes-byte acquisition limit",
                    )
                }
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

internal class BookResourceMaterializationLimitException(
    message: String,
) : IOException(message)
