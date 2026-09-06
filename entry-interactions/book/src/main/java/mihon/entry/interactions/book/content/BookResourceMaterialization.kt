package mihon.entry.interactions.book.content

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

/** Copies acquired bytes within the resource budget and reports transfer progress. */
internal suspend fun copyBookResourceToMaterialization(
    input: InputStream,
    output: File,
    maxBytes: Long,
    totalBytes: Long?,
    onProgress: (bytesRead: Long, totalBytes: Long?) -> Unit,
) = withContext(Dispatchers.IO) {
    output.outputStream().buffered().use { target ->
        val buffer = ByteArray(32 * 1024)
        var copied = 0L
        onProgress(0L, totalBytes)
        while (true) {
            currentCoroutineContext().ensureActive()
            val read = input.read(buffer, 0, minOf(buffer.size.toLong(), maxBytes - copied + 1L).toInt())
            if (read < 0) break
            copied += read
            if (copied > maxBytes) {
                throw BookResourceMaterializationLimitException(
                    "BOOK resource exceeds its $maxBytes-byte acquisition limit",
                )
            }
            target.write(buffer, 0, read)
            onProgress(copied, totalBytes)
        }
    }
}
