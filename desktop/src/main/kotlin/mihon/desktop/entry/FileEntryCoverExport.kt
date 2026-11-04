package mihon.desktop.entry

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.ImageRequest
import coil3.size.Size
import coil3.toBitmap
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.ui.entry.cover.EntryCoverExport
import mihon.desktop.storage.DesktopDirectories
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import tachiyomi.core.common.storage.StorageFileNames
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.storage.DocumentUri
import tachiyomi.i18n.MR
import tachiyomi.i18n.app_name
import tachiyomi.i18n.localized.stringResource
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import mihon.core.platform.PlatformContext as AppPlatformContext

/**
 * Saves the cover as a JPEG into a folder named after the app in the user's pictures folder, as Android's Save to
 * Pictures does, replacing a saved cover of the same name. Without a pictures folder, Save asks for the file to write
 * through the desktop save dialog, which returns a `file://` URI of it. There is no share sheet to offer Share through.
 */
@Inject
class FileEntryCoverExport(
    private val imageLoader: ImageLoader,
    private val directories: DesktopDirectories,
) : EntryCoverExport {
    override val isSharingAvailable: Boolean = false

    override val savesToPickedDocument: Boolean
        get() = directories.pictures == null

    override suspend fun saveToPictures(context: AppPlatformContext, data: Any, name: String) {
        val pictures = checkNotNull(directories.pictures) { "No pictures folder to save into" }
        val jpeg = jpeg(data)
        withIOContext {
            val folder = Files.createDirectories(pictures.resolve(stringResource(MR.strings.app_name)))
            Files.write(folder.resolve(StorageFileNames.buildValidFilename("$name.jpg")), jpeg)
        }
    }

    override suspend fun saveToDocument(context: AppPlatformContext, data: Any, document: DocumentUri) {
        val jpeg = jpeg(data)
        withIOContext { Files.write(Path.of(URI(document.value)), jpeg) }
    }

    override suspend fun share(context: AppPlatformContext, data: Any, name: String) {
        error("Sharing is not offered on this desktop")
    }

    private suspend fun jpeg(data: Any): ByteArray {
        val request = ImageRequest.Builder(PlatformContext.INSTANCE)
            .data(data)
            .size(Size.ORIGINAL)
            .build()
        val image = imageLoader.execute(request).image ?: throw IOException("The image didn't load")
        return Image.makeFromBitmap(image.toBitmap()).encodeToData(EncodedImageFormat.JPEG, JPEG_QUALITY)?.bytes
            ?: throw IOException("The image couldn't be encoded")
    }

    private companion object {
        // Android saves covers at the same quality.
        const val JPEG_QUALITY = 100
    }
}
