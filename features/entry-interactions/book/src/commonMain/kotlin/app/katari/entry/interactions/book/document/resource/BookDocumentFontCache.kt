package app.katari.entry.interactions.book.document.resource

import androidx.compose.ui.text.font.FontFamily
import app.katari.entry.interactions.book.document.resource.font.unpackWebFont
import app.katari.entry.interactions.book.preparation.BookPublicationResourceLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.WeakHashMap

/** Bounded decoded fonts per live publication, without retaining closed resource owners. */
internal object BookDocumentFontCache {
    private val publications = WeakHashMap<BookPublicationResourceLoader, PublicationFonts>()

    suspend fun load(
        loader: BookPublicationResourceLoader,
        resourceId: String,
        generation: Int,
        decoding: BookDocumentResourceDecoding,
    ): FontFamily? {
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
                withContext(Dispatchers.Default) {
                    require(resource.bytes.size in 1..requirement.maxBytes)
                    decoding.fontFamily(unpackWebFont(resource.bytes) ?: resource.bytes)
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
        val fonts = linkedMapOf<String, FontFamily?>()
    }

    private const val MAX_FONTS = 16
}
