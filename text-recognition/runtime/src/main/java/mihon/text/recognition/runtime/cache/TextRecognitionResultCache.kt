package mihon.text.recognition.runtime.cache

import kotlinx.serialization.json.Json
import mihon.text.recognition.api.result.RecognizedTextRegion
import java.io.File
import java.security.MessageDigest

/**
 * Persistent, size-limited store of recognized regions keyed by image content, scope, and pipeline.
 *
 * Entries are small files; reading an entry marks it recently used, and writing evicts the least recently used
 * entries once the directory exceeds [maximumBytes]. Corrupt entries are treated as absent.
 */
internal class TextRecognitionResultCache(
    directory: () -> File,
    private val maximumBytes: Long,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val directory by lazy(directory)
    private val json = Json { ignoreUnknownKeys = true }

    @Synchronized
    fun read(key: TextRecognitionCacheKey): List<RecognizedTextRegion>? {
        val file = file(key)
        if (!file.isFile) return null
        val regions = runCatching {
            json.decodeFromString(CachedTextRecognitionResult.serializer(), file.readText()).toRegions()
        }.getOrNull()
        if (regions == null) {
            file.delete()
            return null
        }
        file.setLastModified(clock())
        return regions
    }

    @Synchronized
    fun write(key: TextRecognitionCacheKey, regions: List<RecognizedTextRegion>) {
        directory.mkdirs()
        val target = file(key)
        val temporary = File(directory, "${target.name}.tmp")
        temporary.writeText(json.encodeToString(CachedTextRecognitionResult.serializer(), regions.toCached()))
        if (!temporary.renameTo(target)) {
            temporary.delete()
            return
        }
        target.setLastModified(clock())
        evict()
    }

    private fun evict() {
        val entries = directory.listFiles { file -> file.isFile && file.name.endsWith(ENTRY_SUFFIX) }.orEmpty()
        var total = entries.sumOf(File::length)
        if (total <= maximumBytes) return
        entries.sortedBy(File::lastModified).forEach { entry ->
            if (total <= maximumBytes) return
            val length = entry.length()
            if (entry.delete()) total -= length
        }
    }

    private fun file(key: TextRecognitionCacheKey): File = File(directory, key.digest + ENTRY_SUFFIX)

    private companion object {
        const val ENTRY_SUFFIX = ".json"
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
