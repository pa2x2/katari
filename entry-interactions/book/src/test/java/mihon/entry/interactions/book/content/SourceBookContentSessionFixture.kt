package mihon.entry.interactions.book.content

import android.app.Application
import eu.kanade.tachiyomi.source.entry.BookResourceCatalog
import eu.kanade.tachiyomi.source.entry.BookResourceHierarchyNode
import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import eu.kanade.tachiyomi.source.entry.BookSourceResource
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.mockk.every
import io.mockk.mockk
import mihon.book.api.BookCatalogCoverage
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookResourceAvailability
import tachiyomi.domain.entry.model.Entry
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
internal abstract class SourceBookContentSessionFixture {
    protected fun session(
        media: EntryMedia.Book,
        source: UnifiedSource = source(),
        resolver: BookExternalResourceResolver = FakeExternalResolver(emptyMap()),
        directory: File = Files.createTempDirectory("katari-book-materialized").toFile(),
        materializationStore: BookMaterializationStore = BookMaterializationCache(
            application(),
            directory,
        ),
    ): SourceBookContentSession {
        return SourceBookContentSession(
            source = source,
            entry = Entry.create().copy(
                id = 1L,
                source = 42L,
                url = "/books/fixture",
                type = EntryType.BOOK,
            ),
            media = media,
            externalResolver = resolver,
            materializationStore = materializationStore,
        )
    }

    protected fun application(): Application = mockk(relaxed = true)

    protected fun source(): EntryCatalogueSource = mockk {
        every { id } returns 42L
        every { name } returns "Fixture"
        every { lang } returns "en"
    }

    protected fun bookMedia(
        resources: List<BookSourceResource> = emptyList(),
        initialResourceId: String? = null,
        initialLocation: BookResourceLocation? = null,
        publicationKeyOverride: String? = null,
        hierarchy: List<BookResourceHierarchyNode> = emptyList(),
    ): EntryMedia.Book {
        return EntryMedia.Book(
            descriptor = BookContentDescriptor("application/vnd.katari.book+json"),
            publicationKeyOverride = publicationKeyOverride,
            publicationRevision = "publication-v2",
            catalog = BookResourceCatalog(
                resources = resources,
                revision = "catalog-v3",
                coverage = BookCatalogCoverage.COMPLETE,
            ),
            hierarchy = hierarchy,
            initialResourceId = initialResourceId,
            initialResourceLocation = initialLocation,
        )
    }

    protected fun resource(
        id: String,
        title: String? = null,
        order: Long? = null,
        groupId: String? = null,
        mediaType: String? = null,
        size: Long? = null,
        availability: BookResourceAvailability = BookResourceAvailability.AVAILABLE,
        location: BookResourceLocation,
    ): BookSourceResource {
        return BookSourceResource(
            id = id,
            title = title,
            order = order,
            groupId = groupId,
            mediaType = mediaType,
            size = size,
            availability = availability,
            location = location,
        )
    }

    protected fun inline(text: String): BookResourceLocation =
        BookResourceLocation.InlineText(text, "text/plain")
}
internal class FakeExternalResolver(
    private val content: Map<String, ByteArray>,
    override val canResolveAppReferences: Boolean = true,
) : BookExternalResourceResolver {
    val requests = mutableListOf<Pair<BookResourceLocation, BookByteRange?>>()
    val closeCount = AtomicInteger()

    override suspend fun open(
        location: BookResourceLocation,
        range: BookByteRange?,
    ): ExternalBookResource {
        requests += location to range
        val bytes = content.getValue(location.key())
        val start = range?.startInclusive?.toInt() ?: 0
        val end = range?.endExclusive?.coerceAtMost(bytes.size.toLong())?.toInt() ?: bytes.size
        val stream = ByteArrayInputStream(bytes, start, end - start)
        return object : ExternalBookResource {
            override val stream: InputStream = stream

            override fun close() {
                stream.close()
                closeCount.incrementAndGet()
            }
        }
    }
}

internal fun BookResourceLocation.key(): String = when (this) {
    is BookResourceLocation.RemoteRequest -> "remote:$url"
    is BookResourceLocation.LocalUri -> "local:$uri"
    is BookResourceLocation.AppReference -> "app:$id"
    is BookResourceLocation.InlineBytes,
    is BookResourceLocation.InlineText,
    is BookResourceLocation.SourceChild,
    -> error("Location is not external: $this")
}
