package mihon.entry.interactions.book.epub

import mihon.book.api.model.BookPublicationModel
import mihon.book.api.model.BookPublicationModelDescriptor
import org.readium.r2.shared.publication.Layout
import org.readium.r2.shared.publication.Publication

/** EPUB model prepared specifically for readers built on the Readium publication engine. */
internal class ReadiumEpubPublicationModel(
    val publication: Publication,
) : BookPublicationModel {
    override val descriptor: BookPublicationModelDescriptor = DESCRIPTOR

    val isFixedLayout: Boolean
        get() = publication.metadata.layout == Layout.FIXED

    companion object {
        val DESCRIPTOR = BookPublicationModelDescriptor(
            id = "book.epub.readium",
            version = 1,
        )
    }
}
