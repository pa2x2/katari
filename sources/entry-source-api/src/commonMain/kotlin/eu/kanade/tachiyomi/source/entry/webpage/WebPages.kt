package eu.kanade.tachiyomi.source.entry.webpage

/** The pages Katari keeps for sources, which it provides and [webPage] asks. Sources don't implement or call it. */
interface WebPages {
    suspend fun <T> use(source: WebPageSource, name: String, block: suspend (page: WebPage) -> T): T
}
