package mihon.entry.interactions.manga.page.cache

import okio.BufferedSink
import java.io.Closeable
import java.io.File

internal class MangaPageStagingWrite(
    private val output: BufferedSink,
    private val commitAction: () -> File,
    private val abortAction: () -> Unit,
) : Closeable {
    private var finished = false

    fun write(bytes: ByteArray, length: Int) {
        check(!finished) { "Page cache staging write is already finished" }
        output.write(bytes, 0, length)
        output.emitCompleteSegments()
    }

    fun commit(): File {
        check(!finished) { "Page cache staging write is already finished" }
        return try {
            output.flush()
            output.close()
            commitAction().also { finished = true }
        } catch (error: Throwable) {
            abortAction()
            finished = true
            throw error
        }
    }

    override fun close() {
        if (finished) return
        try {
            output.close()
        } finally {
            abortAction()
            finished = true
        }
    }
}
