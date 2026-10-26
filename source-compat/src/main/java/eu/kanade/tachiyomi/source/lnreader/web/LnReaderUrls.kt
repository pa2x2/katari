package eu.kanade.tachiyomi.source.lnreader.web

import eu.kanade.tachiyomi.source.lnreader.runtime.LnReaderPluginCalls
import eu.kanade.tachiyomi.source.lnreader.runtime.asText
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Where a plugin's paths lead on its site.
 *
 * LNReader opens a path in WebView as the URL it already is, else as the plugin's `resolveUrl` makes it, else as the
 * site and the path joined. Katari asks for WebView URLs without waiting, and `resolveUrl` is a call into the plugin, so
 * its results are taken at refresh and kept in the memo of the entry and of each chapter; until then the site and the
 * path joined stand in.
 */
internal class LnReaderUrls(
    private val site: String,
    private val calls: LnReaderPluginCalls,
    private val resolvesUrls: Boolean,
) {
    /** The page of [path] for WebView, with [memo] what an earlier [resolve] kept for it. */
    fun pageUrl(path: String, memo: JsonObject): String =
        memo[MEMO_URL].asText() ?: joined(path)

    /**
     * The URL to keep in a memo for each of [paths], a novel's or its chapters', by the plugin's `resolveUrl`; null
     * where joining the site and the path gives it anyway.
     */
    suspend fun resolve(paths: List<String>, isNovel: Boolean): List<String?> {
        if (!resolvesUrls) return paths.map { null }
        val relative = paths.filterNot(::isAbsolute)
        val resolved = relative.zip(calls.resolveUrls(relative, isNovel)).toMap()
        return paths.map { path -> resolved[path]?.takeIf { it != joined(path) } }
    }

    /** A cover as Katari can load it: relative ones against the site; none for LNReader's placeholder or inline data. */
    fun coverUrl(cover: String?): String? {
        if (cover.isNullOrBlank() || cover.equals(DEFAULT_COVER, ignoreCase = true) || cover.startsWith("data:")) {
            return null
        }
        return site.toHttpUrlOrNull()?.resolve(cover)?.toString() ?: cover.takeIf(::isAbsolute)
    }

    private fun joined(path: String): String = when {
        path.startsWith("//") -> "https:$path"
        isAbsolute(path) -> path
        else -> site + path
    }

    companion object {
        /** The memo key of the URL [resolve] found. */
        const val MEMO_URL = "url"

        // What @libs/defaultCover gives plugins with no cover of their own
        private const val DEFAULT_COVER =
            "https://github.com/lnreader/lnreader-plugins/blob/master/public/static/coverNotAvailable.webp?raw=true"

        // LNReader's isUrlAbsolute, which decides what it resolves
        private fun isAbsolute(url: String): Boolean {
            if (url.startsWith("//")) return true
            val scheme = url.indexOf("://")
            val dot = url.indexOf('.')
            val slash = url.indexOf('/')
            if (scheme < 0 || dot < 0 || slash < 0) return false
            if (url.indexOf(':') > slash) return false
            return scheme < dot
        }
    }
}
