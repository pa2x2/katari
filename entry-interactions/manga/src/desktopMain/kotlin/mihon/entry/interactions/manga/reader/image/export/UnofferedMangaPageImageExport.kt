package mihon.entry.interactions.manga.reader.image.export

import mihon.core.platform.PlatformContext
import java.io.InputStream

/** Desktop has no manga reader yet, so no page is saved, shared or copied from it. */
internal object UnofferedMangaPageImageExport : MangaPageImageExport {
    override val offersSaving: Boolean = false
    override val offersSharing: Boolean = false
    override val offersCopying: Boolean = false

    override suspend fun save(context: PlatformContext, image: () -> InputStream, name: String, folder: String?) =
        throw UnsupportedOperationException("Saving reader pages isn't offered on desktop")

    override suspend fun share(context: PlatformContext, image: () -> InputStream, name: String, message: String) =
        throw UnsupportedOperationException("Sharing reader pages isn't offered on desktop")

    override suspend fun copy(context: PlatformContext, image: () -> InputStream, name: String) =
        throw UnsupportedOperationException("Copying reader pages isn't offered on desktop")
}
