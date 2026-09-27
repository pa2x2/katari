package mihon.text.recognition.runtime.cache

import com.jakewharton.disklrucache.DiskLruCache
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import mihon.text.recognition.api.result.RecognizedTextRegion
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * Persistent, size-limited store of recognized regions keyed by image content, scope, and pipeline, evicting the
 * least recently used results first. Unreadable entries are treated as absent.
 */
internal class TextRecognitionResultCache(
    directory: () -> File,
    private val maximumBytes: Long,
) {
    private val cache by lazy { DiskLruCache.open(directory(), CACHE_VERSION, VALUE_COUNT, maximumBytes) }
    private val json = Json { ignoreUnknownKeys = true }

    @Synchronized
    fun read(key: TextRecognitionCacheKey): List<RecognizedTextRegion>? {
        val stored = try {
            cache.get(key.digest)?.use { it.getString(0) }
        } catch (_: IOException) {
            null
        } ?: return null
        return try {
            json.decodeFromString(CachedTextRecognitionResult.serializer(), stored).toRegions()
        } catch (_: SerializationException) {
            cache.remove(key.digest)
            null
        } catch (_: IllegalArgumentException) {
            cache.remove(key.digest)
            null
        }
    }

    @Synchronized
    fun write(key: TextRecognitionCacheKey, regions: List<RecognizedTextRegion>) {
        var editor: DiskLruCache.Editor? = null
        try {
            editor = cache.edit(key.digest) ?: return
            editor.set(0, json.encodeToString(CachedTextRecognitionResult.serializer(), regions.toCached()))
            editor.commit()
        } catch (_: IOException) {
            editor?.abortUnlessCommitted()
        }
    }

    private companion object {
        /** Bump when the stored representation changes incompatibly. */
        const val CACHE_VERSION = 1
        const val VALUE_COUNT = 1
    }
}

/** Identity of one recognition run. Any change to the image, the scope, or the pipeline produces a new key. */
internal data class TextRecognitionCacheKey(
    val parts: List<String>,
) {
    val digest: String by lazy {
        val digest = MessageDigest.getInstance("SHA-256")
        parts.forEach { part ->
            digest.update(part.toByteArray(Charsets.UTF_8))
            digest.update(0)
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }
}
