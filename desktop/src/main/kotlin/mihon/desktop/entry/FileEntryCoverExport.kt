package mihon.desktop.entry

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.ImageRequest
import coil3.size.Size
import coil3.toBitmap
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.ui.entry.cover.EntryCoverExport
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.storage.DocumentUri
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import mihon.core.platform.PlatformContext as AppPlatformContext

/**
 * Saves the cover as a JPEG into the plain file the desktop save dialog returns a `file://` URI of. Desktops have no
 * shared pictures folder in common, so Save always asks where to; there is no share sheet to offer Share through.
 */
@Inject
class FileEntryCoverExport(private val imageLoader: ImageLoader) : EntryCoverExport {
    override val isSharingAvailable: Boolean = false

    override val savesToPickedDocument: Boolean = true

    override suspend fun saveToPictures(context: AppPlatformContext, data: Any, name: String) {
        error("Desktop saves covers into a file the user picks")
    }

    override suspend fun saveToDocument(context: AppPlatformContext, data: Any, document: DocumentUri) {
        val request = ImageRequest.Builder(PlatformContext.INSTANCE)
            .data(data)
            .size(Size.ORIGINAL)
            .build()
        val image = imageLoader.execute(request).image ?: throw IOException("The image didn't load")
        val jpeg = Image.makeFromBitmap(image.toBitmap()).encodeToData(EncodedImageFormat.JPEG, JPEG_QUALITY)
            ?: throw IOException("The image couldn't be encoded")
        withIOContext { Files.write(Path.of(URI(document.value)), jpeg.bytes) }
    }

    override suspend fun share(context: AppPlatformContext, data: Any, name: String) {
        error("Sharing is not offered on this desktop")
    }

    private companion object {
        // Android saves covers at the same quality.
        const val JPEG_QUALITY = 100
    }
}
