package mihon.entry.interactions.manga.reader.image.export

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.core.content.getSystemService
import mihon.entry.interactions.reader.image.ReaderImage
import mihon.entry.interactions.reader.image.ReaderImageLocation
import mihon.entry.interactions.reader.image.ReaderImageSaver
import mihon.entry.interactions.reader.image.toReaderShareIntent
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withUIContext
import java.io.InputStream

/**
 * Saves pages through MediaStore with a notification of the saved picture. Sharing and copying first copy the image
 * into the app's cache, as a page may come from inside an archive with no file of its own; only the last one is kept.
 */
internal class AndroidMangaPageImageExport(private val application: Application) : MangaPageImageExport {
    private val saver = ReaderImageSaver(application)

    override val offersSaving: Boolean = true
    override val offersSharing: Boolean = true
    override val offersCopying: Boolean = true

    override suspend fun save(context: Context, image: () -> InputStream, name: String, folder: String?) {
        val notifier = SavedPageNotifier(application)
        notifier.onClear()
        try {
            val uri = withIOContext {
                saver.save(ReaderImage(image, name, ReaderImageLocation.Pictures.create(folder.orEmpty())))
            }
            withUIContext { notifier.onComplete(uri) }
        } catch (e: Throwable) {
            notifier.onError(e.message)
            throw e
        }
    }

    override suspend fun share(context: Context, image: () -> InputStream, name: String, message: String) {
        val uri = saveToCache(image, name)
        withUIContext { context.startActivity(uri.toReaderShareIntent(context, message = message)) }
    }

    override suspend fun copy(context: Context, image: () -> InputStream, name: String) {
        val uri = saveToCache(image, name)
        val clipboard = application.getSystemService<ClipboardManager>() ?: return
        withUIContext { clipboard.setPrimaryClip(ClipData.newUri(application.contentResolver, "", uri)) }
    }

    private suspend fun saveToCache(image: () -> InputStream, name: String): Uri = withIOContext {
        ReaderImageLocation.Cache.directory(application).deleteRecursively()
        saver.save(ReaderImage(image, name, ReaderImageLocation.Cache))
    }
}
