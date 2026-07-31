package mihon.entry.interactions.book.content

import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import java.io.InputStream

/**
 * Katari-side resolver for locations which cannot be opened from inline source data.
 *
 * Implementations own authentication, network clients, content permissions,
 * app-reference lookup, and any durable cache. The returned stream must contain
 * exactly the requested [range] when one is supplied.
 */
internal interface BookExternalResourceResolver {
    val canResolveAppReferences: Boolean
        get() = false

    suspend fun open(location: BookResourceLocation, range: BookByteRange?): ExternalBookResource
}

/** Scoped external stream. Closing it must cancel/release all underlying I/O. */
internal interface ExternalBookResource : AutoCloseable {
    val stream: InputStream
    val mediaType: String?
        get() = null
}

internal class SimpleExternalBookResource(
    override val stream: InputStream,
) : ExternalBookResource {
    override fun close() = stream.close()
}
