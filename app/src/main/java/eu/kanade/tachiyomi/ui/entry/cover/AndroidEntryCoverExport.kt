package eu.kanade.tachiyomi.ui.entry.cover

import android.net.Uri
import app.katari.core.platform.PlatformContext
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.size.Size
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.saver.Image
import eu.kanade.tachiyomi.data.saver.ImageSaver
import eu.kanade.tachiyomi.data.saver.Location
import eu.kanade.tachiyomi.util.system.getBitmapOrNull
import eu.kanade.tachiyomi.util.system.toShareIntent
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.domain.storage.DocumentUri

/**
 * Saves into the device's pictures through [ImageSaver], and shares a copy written to the cache's shared image folder
 * through the system share sheet. An image that doesn't decode to a bitmap, such as an animated cover, is skipped.
 */
@Inject
class AndroidEntryCoverExport(private val imageSaver: ImageSaver) : EntryCoverExport {
    override val isSharingAvailable: Boolean = true

    override val savesToPickedDocument: Boolean = false

    override suspend fun saveToPictures(context: PlatformContext, data: Any, name: String) {
        save(context, data, name, Location.Pictures.create())
    }

    override suspend fun saveToDocument(context: PlatformContext, data: Any, document: DocumentUri) {
        error("Covers are saved into the pictures folder on Android")
    }

    override suspend fun share(context: PlatformContext, data: Any, name: String) {
        val uri = save(context, data, name, Location.Cache) ?: return
        withUIContext {
            context.startActivity(uri.toShareIntent(context))
        }
    }

    /** @return the uri of the saved file, or null when the image doesn't decode to a bitmap. */
    private suspend fun save(context: PlatformContext, data: Any, name: String, location: Location): Uri? {
        val req = ImageRequest.Builder(context)
            .data(data)
            .size(Size.ORIGINAL)
            .build()

        return withIOContext {
            val result = context.imageLoader.execute(req).image?.asDrawable(context.resources)

            // TODO: Handle animated cover
            val bitmap = result?.getBitmapOrNull() ?: return@withIOContext null
            imageSaver.save(
                Image.Cover(
                    bitmap = bitmap,
                    name = name,
                    location = location,
                ),
            )
        }
    }
}
