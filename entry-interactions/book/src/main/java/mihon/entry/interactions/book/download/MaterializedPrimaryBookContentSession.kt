package mihon.entry.interactions.book.download

import mihon.book.api.BookContentResource
import mihon.entry.interactions.book.BookByteRange
import mihon.entry.interactions.book.BookContentSession
import mihon.entry.interactions.book.MaterializedBookResource
import mihon.entry.interactions.book.OpenedBookResource
import java.io.FileInputStream
import java.io.FilterInputStream
import java.io.InputStream

internal class MaterializedPrimaryBookContentSession(
    private val delegate: BookContentSession,
    private val primaryResourceId: String,
    private val primaryResource: MaterializedBookResource,
) : BookContentSession by delegate {
    override suspend fun getResource(resourceId: String) =
        if (resourceId == primaryResourceId) {
            Result.success(primaryResource.metadata)
        } else {
            delegate.getResource(resourceId)
        }

    override suspend fun openResource(
        resourceId: String,
        range: BookByteRange?,
    ): Result<OpenedBookResource> {
        if (resourceId != primaryResourceId) return delegate.openResource(resourceId, range)
        return runCatching {
            val fileLength = primaryResource.file.length()
            val start = range?.startInclusive ?: 0L
            val end = range?.endExclusive ?: fileLength
            require(start in 0..fileLength && end in start..fileLength) {
                "Requested BOOK range is outside the materialized primary resource"
            }
            val input = FileInputStream(primaryResource.file)
            input.channel.position(start)
            MaterializedPrimaryOpenedBookResource(
                metadata = primaryResource.metadata,
                stream = input.buffered().let { stream ->
                    if (range == null) stream else stream.limitedTo(end - start)
                },
            )
        }
    }

    override suspend fun materializeResource(resourceId: String): Result<MaterializedBookResource> =
        if (resourceId == primaryResourceId) {
            Result.success(BorrowedMaterializedBookResource(primaryResource))
        } else {
            delegate.materializeResource(resourceId)
        }

    override fun close() = Unit
}

private class MaterializedPrimaryOpenedBookResource(
    override val metadata: BookContentResource,
    override val stream: InputStream,
) : OpenedBookResource {
    override fun close() = stream.close()
}

private class BorrowedMaterializedBookResource(
    private val delegate: MaterializedBookResource,
) : MaterializedBookResource {
    override val metadata
        get() = delegate.metadata
    override val file
        get() = delegate.file

    override fun invalidate() = delegate.invalidate()
    override fun close() = Unit
}

private fun InputStream.limitedTo(byteCount: Long): InputStream =
    LimitedPrimaryInputStream(this, byteCount)

private class LimitedPrimaryInputStream(
    delegate: InputStream,
    private var remaining: Long,
) : FilterInputStream(delegate) {
    override fun read(): Int {
        if (remaining <= 0L) return -1
        return super.read().also { if (it >= 0) remaining-- }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (remaining <= 0L) return -1
        val read = super.read(buffer, offset, minOf(length.toLong(), remaining).toInt())
        if (read > 0) remaining -= read
        return read
    }
}
