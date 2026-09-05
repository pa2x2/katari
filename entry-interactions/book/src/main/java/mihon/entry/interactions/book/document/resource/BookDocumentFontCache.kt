package mihon.entry.interactions.book.document.resource

import android.graphics.Typeface
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mihon.entry.interactions.book.preparation.BookPublicationResourceLoader
import java.io.File
import java.util.WeakHashMap

/** Bounded decoded fonts per live publication, without retaining closed resource owners. */
internal object BookDocumentFontCache {
    private val publications = WeakHashMap<BookPublicationResourceLoader, PublicationFonts>()

    suspend fun load(
        loader: BookPublicationResourceLoader,
        resourceId: String,
        generation: Int,
        directory: File,
    ): Typeface? {
        val cache = synchronized(publications) { publications.getOrPut(loader) { PublicationFonts() } }
        return cache.mutex.withLock {
            if (cache.generation != generation) {
                cache.fonts.clear()
                cache.generation = generation
            }
            if (resourceId in cache.fonts) return@withLock cache.fonts[resourceId]
            val font = try {
                val requirement = PROSE_FONT_RESOURCE_REQUIREMENT
                val resource = loader.load(
                    resourceId,
                    requirement.acceptedMediaTypes,
                    requirement.maxBytes,
                ).getOrThrow()
                withContext(Dispatchers.IO) {
                    require(resource.bytes.size in 1..requirement.maxBytes)
                    val file = File.createTempFile("book-font-", ".sfnt", directory)
                    try {
                        file.writeBytes(resource.bytes)
                        createValidatedProseTypeface(file)
                    } finally {
                        file.delete()
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            }
            if (cache.fonts.size >= MAX_FONTS) cache.fonts.remove(cache.fonts.keys.first())
            cache.fonts[resourceId] = font
            font
        }
    }

    private class PublicationFonts {
        val mutex = Mutex()
        var generation = -1
        val fonts = linkedMapOf<String, Typeface?>()
    }

    private const val MAX_FONTS = 16
}
