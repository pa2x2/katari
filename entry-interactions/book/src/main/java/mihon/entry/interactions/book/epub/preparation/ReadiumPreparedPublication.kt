package mihon.entry.interactions.book.epub

import mihon.book.api.BookLocator
import mihon.book.api.BookPublication
import mihon.entry.interactions.book.BookPublicationResourceLoader
import mihon.entry.interactions.book.BookSessionCloseStack
import mihon.entry.interactions.book.MaterializedBookResource
import mihon.entry.interactions.book.PreparedBookPublication
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positions
import kotlin.math.abs

/** Owns a prepared Readium model and its materialized EPUB lease. */
internal class ReadiumPreparedPublication(
    enginePublication: Publication,
    lease: MaterializedBookResource,
    publicationId: String,
    revision: String,
    override val resourceLoader: BookPublicationResourceLoader,
) : PreparedBookPublication {
    private val closeStack = BookSessionCloseStack().apply {
        own(lease)
        own(AutoCloseable(enginePublication::close))
    }

    override val model = ReadiumEpubPublicationModel(enginePublication)

    override val publication: BookPublication = ReadiumPublicationAdapter.adapt(
        publication = enginePublication,
        publicationId = publicationId,
        revision = revision,
    )

    override fun validate(locator: BookLocator): Boolean =
        ReadiumLocatorAdapter.restore(locator, model.publication) != null

    override suspend fun reconcileMigratedLocator(locator: BookLocator): BookLocator? {
        if (validate(locator)) return locator
        val totalProgression = locator.totalProgression ?: return null
        return model.publication.positions()
            .filter { it.locations.totalProgression != null }
            .minByOrNull { abs(checkNotNull(it.locations.totalProgression) - totalProgression) }
            ?.let(ReadiumLocatorAdapter::adapt)
    }

    override fun close() = closeStack.close()
}
