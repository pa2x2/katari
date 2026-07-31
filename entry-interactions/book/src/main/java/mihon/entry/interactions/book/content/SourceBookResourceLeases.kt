package mihon.entry.interactions.book.content

import mihon.book.api.BookContentResource
import java.io.File
import java.io.InputStream

internal class SessionOpenedBookResource(
    override val metadata: BookContentResource,
    override val stream: InputStream,
    private val delegate: ExternalBookResource,
    private val onClose: (AutoCloseable) -> Unit,
) : OpenedBookResource {
    private var closed = false

    override fun close() {
        synchronized(this) {
            if (closed) return
            closed = true
        }
        try {
            delegate.close()
        } finally {
            onClose(this)
        }
    }
}

internal class SessionMaterializedBookResource(
    private val delegate: MaterializedBookResource,
    private val onClose: (AutoCloseable) -> Unit,
) : MaterializedBookResource {
    override val metadata: BookContentResource
        get() = delegate.metadata
    override val file: File
        get() = delegate.file
    private var closed = false

    override fun invalidate() = delegate.invalidate()

    override fun close() {
        synchronized(this) {
            if (closed) return
            closed = true
        }
        try {
            delegate.close()
        } finally {
            onClose(this)
        }
    }
}
