package mihon.entry.interactions

import okhttp3.Call
import okhttp3.Headers

/** Feature-owned source network access used specifically for Entry cover requests. */
interface EntryCoverNetworkFeature {
    fun resolve(sourceId: Long): EntryCoverNetworkResolution
}

sealed interface EntryCoverNetworkResolution {
    val sourceId: Long

    data class Available(
        override val sourceId: Long,
        val callFactory: Call.Factory,
        val headers: Headers,
    ) : EntryCoverNetworkResolution

    data class Missing(override val sourceId: Long) : EntryCoverNetworkResolution
    data class Unsupported(override val sourceId: Long) : EntryCoverNetworkResolution
    data class Failed(override val sourceId: Long, val cause: Throwable) : EntryCoverNetworkResolution
}
