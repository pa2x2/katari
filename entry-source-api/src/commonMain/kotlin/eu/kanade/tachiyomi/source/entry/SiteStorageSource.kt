package eu.kanade.tachiyomi.source.entry

/**
 * Optional capability for a source whose site keeps a login or other state in the browser's web storage, which, unlike
 * cookies, Katari's client never sees. While the user browses the source's site in Katari's WebView, Katari reads the
 * page's storage after each page load and hands the last reading over when the WebView closes.
 */
interface SiteStorageSource : UnifiedSource {

    /** Called on the main thread as Katari's WebView for this source closes; keep it short. */
    fun saveSiteStorage(storage: SiteStorage)
}

/**
 * A page's web storage as it was after the last page load.
 *
 * @property url the page the storage was read on; it may be another site the user went on to, such as a login provider.
 * @property local the page's `localStorage`.
 * @property session the page's `sessionStorage`.
 */
class SiteStorage(
    val url: String,
    val local: Map<String, String>,
    val session: Map<String, String>,
)
