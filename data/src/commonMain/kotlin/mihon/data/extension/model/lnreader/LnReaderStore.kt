package mihon.data.extension.model.lnreader

import mihon.domain.extension.model.ExtensionStore
import mihon.domain.extension.model.ExtensionStoreFormat
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * The store of an LNReader index at [indexUrl]. The index is a bare array with no name or contact, and LNReader indexes
 * live on GitHub, so the store is named after the repository's owner and links to the repository; an index elsewhere
 * is named after its host.
 */
internal fun lnReaderStore(indexUrl: String): ExtensionStore {
    val url = indexUrl.toHttpUrl()
    val segments = url.pathSegments.filter { it.isNotEmpty() }
    val repository = segments.takeIf { url.host in GITHUB_HOSTS && it.size >= 2 }?.take(2)
    val name = repository?.first() ?: url.host
    return ExtensionStore(
        indexUrl = indexUrl,
        name = name,
        badgeLabel = name.take(BADGE_LENGTH),
        signingKey = null,
        contact = ExtensionStore.Contact(
            website =
            repository?.let { (owner, repo) -> "https://github.com/$owner/$repo" } ?: "${url.scheme}://${url.host}",
            discord = null,
        ),
        format = ExtensionStoreFormat.LNREADER,
        extensionListUrl = null,
    )
}

private val GITHUB_HOSTS = setOf("raw.githubusercontent.com", "github.com")

private const val BADGE_LENGTH = 3
