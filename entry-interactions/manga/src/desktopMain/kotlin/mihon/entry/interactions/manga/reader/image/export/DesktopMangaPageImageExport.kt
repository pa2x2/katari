package mihon.entry.interactions.manga.reader.image.export

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.unit.IntRect
import mihon.core.image.asImageBitmap
import mihon.core.image.decode.ImageRegionDecoder
import mihon.core.image.decode.detectImageType
import mihon.core.image.progressive.ProgressiveImageFormat
import mihon.core.image.release
import mihon.core.platform.PlatformContext
import mihon.core.platform.clipboard.ImageClipboard
import mihon.core.platform.storage.UserPicturesFolder
import mihon.entry.interactions.manga.reader.image.source.TYPE_HEADER_BYTES
import tachiyomi.core.common.storage.StorageFileNames
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.i18n.MR
import tachiyomi.i18n.app_name
import tachiyomi.i18n.localized.stringResource
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files

/**
 * Saves pages as they were downloaded into a folder named after the app in the user's pictures folder, as Android's
 * Save does, replacing a saved page of the same name; without a pictures folder, Save isn't offered. Copy decodes the
 * page, the first frame of an animated one, onto the clipboard. There is no share sheet to offer Share through.
 */
internal class DesktopMangaPageImageExport(
    private val pictures: UserPicturesFolder,
    private val clipboard: ImageClipboard,
) : MangaPageImageExport {
    override val offersSaving: Boolean get() = pictures.path() != null
    override val offersSharing: Boolean = false
    override val offersCopying: Boolean = true

    override suspend fun save(context: PlatformContext, image: () -> InputStream, name: String, folder: String?) {
        val root = checkNotNull(pictures.path()) { "No pictures folder to save into" }
        withIOContext {
            val bytes = image().use { it.readBytes() }
            val type = detectImageType(bytes.copyOf(minOf(bytes.size, TYPE_HEADER_BYTES)))
                ?: throw IOException("Not an image")
            var directory = root.resolve(stringResource(MR.strings.app_name))
            if (!folder.isNullOrEmpty()) directory = directory.resolve(folder)
            Files.createDirectories(directory)
            val extension = if (type.format == ProgressiveImageFormat.JPEG) "jpg" else type.format.name.lowercase()
            val file = StorageFileNames.buildValidFilename("$name.$extension")
            Files.write(directory.resolve(file), bytes)
        }
    }

    override suspend fun share(context: PlatformContext, image: () -> InputStream, name: String, message: String) =
        error("Sharing is not offered on the desktop")

    override suspend fun copy(context: PlatformContext, image: () -> InputStream, name: String) {
        val awt = withIOContext {
            val decoder = ImageRegionDecoder.open(image()) ?: throw IOException("The page couldn't be decoded")
            decoder.use {
                val bitmap = it.decode(IntRect(0, 0, it.width, it.height), sampleSize = 1)
                    ?: throw IOException("The page couldn't be decoded")
                try {
                    bitmap.asImageBitmap().toAwtImage()
                } finally {
                    bitmap.release()
                }
            }
        }
        clipboard.copy(awt)
    }
}
