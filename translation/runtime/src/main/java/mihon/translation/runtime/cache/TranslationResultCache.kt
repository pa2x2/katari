package mihon.translation.runtime.cache

import com.jakewharton.disklrucache.DiskLruCache
import mihon.translation.api.request.ResolvedTranslationRequest
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * Persistent, size-limited store of inline translations keyed by engine, language pair, and exact text.
 *
 * Readers translate the same text again and again (reopened chapters, recognized pages), so answers are kept across
 * sessions; the least recently used ones are evicted first. Storage failures only disable reuse, never translation.
 */
class TranslationResultCache(
    directory: () -> File,
    private val maximumBytes: Long,
) {
    private val cache by lazy { DiskLruCache.open(directory(), CACHE_VERSION, VALUE_COUNT, maximumBytes) }

    @Synchronized
    fun get(request: ResolvedTranslationRequest): String? = try {
        cache.get(request.key())?.use { it.getString(0) }?.takeIf(String::isNotBlank)
    } catch (_: IOException) {
        null
    }

    @Synchronized
    fun put(request: ResolvedTranslationRequest, translatedText: String) {
        var editor: DiskLruCache.Editor? = null
        try {
            editor = cache.edit(request.key()) ?: return
            editor.set(0, translatedText)
            editor.commit()
        } catch (_: IOException) {
            editor?.abortUnlessCommitted()
        }
    }

    private fun ResolvedTranslationRequest.key(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        listOf(engine.value, sourceLanguage.value, targetLanguage.value, text).forEach { part ->
            digest.update(part.toByteArray(Charsets.UTF_8))
            digest.update(0)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        /** Bump when cached values stop matching what engines would return. */
        const val CACHE_VERSION = 1
        const val VALUE_COUNT = 1
    }
}
