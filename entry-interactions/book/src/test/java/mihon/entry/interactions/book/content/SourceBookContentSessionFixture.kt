package mihon.entry.interactions.book.content

import android.app.Application
import eu.kanade.tachiyomi.source.entry.BookResourceCatalog
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
import tachiyomi.domain.entry.model.Entry
import java.nio.file.Files
internal abstract class SourceBookContentSessionFixture {
    protected fun session(
        media: EntryMedia.Book,
        source: UnifiedSource = source(),
        resolver: BookExternalResourceResolver = UnusedExternalResolver,
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
            materializationStore = BookMaterializationCache(
                mockk<Application>(relaxed = true),
                Files.createTempDirectory("katari-book-materialized").toFile(),
            ),
        )
    }

    protected fun source(): EntryCatalogueSource = mockk {
        every { id } returns 42L
        every { name } returns "Fixture"
        every { lang } returns "en"
    }

    protected fun bookMedia(
        resources: List<BookSourceResource> = emptyList(),
        publicationKeyOverride: String? = null,
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
        )
    }

    protected fun resource(id: String, location: BookResourceLocation): BookSourceResource =
        BookSourceResource(id = id, location = location)
}

/** Resolver for sessions whose resources never leave the source. */
private object UnusedExternalResolver : BookExternalResourceResolver {
    override suspend fun open(location: BookResourceLocation, range: BookByteRange?): ExternalBookResource =
        error("Unexpected external access to $location")
}
