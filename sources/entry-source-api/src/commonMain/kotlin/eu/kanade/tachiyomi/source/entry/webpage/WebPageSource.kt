package eu.kanade.tachiyomi.source.entry.webpage

import eu.kanade.tachiyomi.source.entry.UnifiedSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Optional capability for a source whose site only works with its scripts running. Katari runs the site's pages in a
 * web view nobody sees and keeps each one between the source's calls, so the site's app loads once and the source
 * navigates inside it. Implementing this tells Katari the source needs a web view.
 *
 * A source reaches a page through [webPage], never by holding one: Katari closes a page that sat idle, when memory is
 * short or when its renderer ended, and makes a new one for the next call.
 */
interface WebPageSource : UnifiedSource {

    /** What the page [name] is. Called each time Katari makes that page. */
    fun webPageSpec(name: String): WebPageSpec

    /**
     * Brings [page], a new page [name] that has loaded [WebPageSpec.url], to the state the source's calls expect, such
     * as waiting for the site's app to start. Katari calls it again for every new page, and for a kept page whose
     * profile changed, as the source's settings may differ between profiles.
     */
    suspend fun prepareWebPage(name: String, page: WebPage)
}

/**
 * Runs [block] on the kept page [name], making and [preparing][WebPageSource.prepareWebPage] it first where Katari
 * holds none. One caller has a page at a time: a second call for the same page waits for the first to return. A source
 * that must work on two routes at once gives the second page another name; Katari keeps few pages per source and
 * fails the call for one more.
 *
 * [page] must not be used after [block] returns, and [block] must not ask for the page it runs on again, which would
 * wait for itself.
 *
 * @throws WebPageException where no page can be made, such as without a web view.
 */
suspend fun <T> WebPageSource.webPage(name: String = "main", block: suspend (page: WebPage) -> T): T =
    Injekt.get<WebPages>().use(this, name, block)
