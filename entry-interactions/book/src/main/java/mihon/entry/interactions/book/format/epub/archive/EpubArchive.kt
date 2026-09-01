package mihon.entry.interactions.book.format.epub.archive

import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/** Path-confined access to one materialized publication container. */
internal class EpubArchive(file: File) : AutoCloseable {
    private val zip = ZipFile(file)
    private val entries: Map<String, ZipEntry> = buildMap {
        zip.entries().asSequence().filterNot(ZipEntry::isDirectory).forEach { entry ->
            val path = normalizeArchivePath(entry.name)
            require(put(path, entry) == null) { "Publication archive contains duplicate path $path" }
        }
    }

    fun contains(path: String): Boolean = normalizeArchivePath(path) in entries

    fun read(path: String, maxBytes: Int): ByteArray {
        val normalized = normalizeArchivePath(path)
        val entry = requireNotNull(entries[normalized]) { "Publication resource is missing: $normalized" }
        require(entry.size < 0L || entry.size <= maxBytes) { "Publication resource exceeds its byte limit" }
        return zip.getInputStream(entry).use { input ->
            val output = java.io.ByteArrayOutputStream(minOf(maxBytes, DEFAULT_BUFFER_SIZE))
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                require(total <= maxBytes) { "Publication resource exceeds its byte limit" }
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        }
    }

    fun readText(path: String, maxBytes: Int): String = read(path, maxBytes).toString(StandardCharsets.UTF_8)

    override fun close() = zip.close()
}

internal fun resolveArchiveReference(baseResource: String, reference: String): EpubArchiveReference? {
    val trimmed = reference.trim()
    if (trimmed.isEmpty()) return null
    val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
    if (uri.isAbsolute) return EpubArchiveReference.External(trimmed)
    val fragment = uri.rawFragment?.percentDecoded()?.takeIf(String::isNotBlank)
    val rawPath = uri.rawPath.orEmpty()
    val resolved = if (rawPath.isEmpty()) {
        normalizeArchivePath(baseResource)
    } else {
        val baseDirectory = normalizeArchivePath(baseResource).substringBeforeLast('/', "")
        normalizeArchivePath(
            listOf(baseDirectory, rawPath.percentDecoded()).filter(String::isNotEmpty).joinToString("/"),
        )
    }
    return EpubArchiveReference.Internal(resolved, fragment)
}

internal fun normalizeArchivePath(path: String): String {
    require(path.isNotBlank()) { "Publication archive path must not be blank" }
    require(!path.startsWith('/') && !path.startsWith('\\')) { "Publication archive path must be relative" }
    require('\\' !in path && '\u0000' !in path) { "Publication archive path contains unsafe characters" }
    val normalized = ArrayDeque<String>()
    path.split('/').forEach { segment ->
        when (segment) {
            "", "." -> Unit
            ".." -> {
                require(normalized.isNotEmpty()) { "Publication archive path escapes its container" }
                normalized.removeLast()
            }
            else -> normalized.add(segment)
        }
    }
    require(normalized.isNotEmpty()) { "Publication archive path must identify a resource" }
    return normalized.joinToString("/")
}

internal sealed interface EpubArchiveReference {
    data class Internal(val path: String, val fragment: String?) : EpubArchiveReference
    data class External(val url: String) : EpubArchiveReference
}

private fun String.percentDecoded(): String {
    val output = java.io.ByteArrayOutputStream(length)
    var index = 0
    while (index < length) {
        val character = this[index]
        if (character == '%' && index + 2 < length) {
            val value = substring(index + 1, index + 3).toIntOrNull(16)
            if (value != null) {
                output.write(value)
                index += 3
                continue
            }
        }
        output.write(character.toString().toByteArray(StandardCharsets.UTF_8))
        index += 1
    }
    return output.toString(StandardCharsets.UTF_8.name())
}
