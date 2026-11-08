package mihon.entry.interactions.book.download

internal data class BookDownloadResourceBudget(
    val maxResourceCount: Int = 128,
    val maxEncodedBytes: Long = 256L * 1024L * 1024L,
) {
    init {
        require(maxResourceCount > 0)
        require(maxEncodedBytes > 0)
    }

    fun tracker() = BookDownloadResourceBudgetTracker(maxEncodedBytes)
}

internal class BookDownloadResourceBudgetTracker(
    private val maxEncodedBytes: Long,
) {
    private var encodedBytes = 0L
    val remainingEncodedBytes: Long
        get() = maxEncodedBytes - encodedBytes

    fun requireCanInclude(byteCount: Long, resourceId: String) {
        require(byteCount >= 0L)
        if (byteCount > maxEncodedBytes - encodedBytes) {
            throw BookResourceBudgetException(
                "BOOK resource $resourceId would exceed the $maxEncodedBytes-byte download limit.",
            )
        }
    }

    fun include(byteCount: Long, resourceId: String) {
        requireCanInclude(byteCount, resourceId)
        encodedBytes += byteCount
    }
}
