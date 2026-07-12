package mihon.entry.interactions.book

internal class BookSessionCloseStack : AutoCloseable {
    private var closed = false
    private val resources = ArrayDeque<AutoCloseable>()

    @Synchronized
    fun own(resource: AutoCloseable) {
        check(!closed) { "Session is already closed" }
        resources.addFirst(resource)
    }

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        var firstFailure: Throwable? = null
        resources.forEach { resource ->
            try {
                resource.close()
            } catch (error: Throwable) {
                if (firstFailure == null) firstFailure = error else firstFailure.addSuppressed(error)
            }
        }
        resources.clear()
        firstFailure?.let { throw it }
    }
}
