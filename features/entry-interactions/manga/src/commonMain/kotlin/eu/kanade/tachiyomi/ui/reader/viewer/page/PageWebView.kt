package eu.kanade.tachiyomi.ui.reader.viewer.page

import eu.kanade.tachiyomi.ui.reader.model.ReaderPage

/**
 * Opens [page]'s image address in a web view through [openPageInWebView], which is null where the platform can't; null
 * too when the page has no address on the web. [sourceId] lets the web view use the source's cookies and headers.
 */
internal fun pageWebViewAction(
    page: ReaderPage,
    sourceId: Long?,
    openPageInWebView: ((url: String, sourceId: Long?) -> Unit)?,
): (() -> Unit)? {
    val open = openPageInWebView ?: return null
    val url = page.imageUrl?.takeIf { it.startsWith("http", ignoreCase = true) } ?: return null
    return { open(url, sourceId) }
}
