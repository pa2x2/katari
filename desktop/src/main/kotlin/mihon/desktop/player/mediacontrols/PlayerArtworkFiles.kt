package mihon.desktop.player.mediacontrols

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.ImageRequest
import coil3.toBitmap
import eu.kanade.tachiyomi.data.coil.SourceImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import mihon.desktop.storage.DesktopDirectories
import mihon.entry.interactions.player.AnimePlayerMediaArtwork
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

/**
 * The playing episode's artwork as a PNG in the runtime directory, which shells read by its `file://` URI, MPRIS's
 * one way to hand over an image the app loaded itself: through the image loader, with the source's own headers, which
 * a shell fetching the source's URL wouldn't send. Each episode gets a file of its own, as shells keep an image by its
 * URI; the file goes when its loading is closed.
 */
internal class PlayerArtworkFiles(
    private val imageLoader: ImageLoader,
    private val directories: DesktopDirectories,
    private val scope: CoroutineScope,
) {
    /**
     * Loads [artwork] in the background and passes [onWritten] the file's URI once it is written, unless closed before;
     * artwork that doesn't load is left out.
     */
    fun write(artwork: AnimePlayerMediaArtwork, onWritten: (String) -> Unit): AutoCloseable {
        val file = directories.runtime.resolve("player-artwork-${files.incrementAndGet()}.png")
        val job = scope.launch(Dispatchers.IO) {
            val png = artwork.models().firstNotNullOfOrNull { png(it) } ?: return@launch
            try {
                Files.createDirectories(file.parent)
                Files.write(file, png)
                onWritten(file.toUri().toString())
            } catch (e: IOException) {
                logcat(LogPriority.WARN, e) { "Couldn't write the episode's artwork for the media controls" }
            }
        }
        return AutoCloseable {
            job.cancel()
            scope.launch(Dispatchers.IO + NonCancellable) {
                job.join()
                delete(file)
            }
        }
    }

    private fun AnimePlayerMediaArtwork.models(): List<Any> = listOfNotNull(
        episodePreviewUrl?.let { SourceImage(cover.sourceId, it) },
        cover,
    )

    private suspend fun png(model: Any): ByteArray? {
        val request = ImageRequest.Builder(PlatformContext.INSTANCE)
            .data(model)
            .size(ARTWORK_SIZE)
            .build()
        val image = imageLoader.execute(request).image ?: return null
        return withContext(Dispatchers.Default) {
            Image.makeFromBitmap(image.toBitmap()).encodeToData(EncodedImageFormat.PNG)?.bytes
        }
    }

    private fun delete(file: Path) {
        try {
            Files.deleteIfExists(file)
        } catch (e: IOException) {
            logcat(LogPriority.DEBUG, e) { "Couldn't remove the media controls' artwork" }
        }
    }

    private companion object {
        // Each loading's file has a name of its own, so a file let go of is never one a later loading writes
        val files = AtomicInteger()

        // Shells draw it at most a few hundred pixels across, even on a scaled display
        const val ARTWORK_SIZE = 512
    }
}
