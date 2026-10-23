package mihon.video.engine.mpv.proxy

/**
 * Points a DASH manifest's absolute and root-relative URLs through the proxy: `BaseURL` elements and the `media`,
 * `initialization`, `sourceURL` and `index` attributes. Relative ones stay as they are: mpv resolves them against the
 * manifest's own proxy URL, which keeps the upstream path, or against a `BaseURL` that is rewritten here. Templates such
 * as `$Number$` survive, because only the scheme and host of a URL change.
 *
 * When the manifest came from another URL than the one asked for (a redirect) and has no `BaseURL` of its own,
 * [redirectedBase] is added as the manifest's first `BaseURL`, so relative references resolve against where it is.
 */
internal fun rewriteDashManifest(manifest: String, redirectedBase: String?, rewrite: (String) -> String?): String {
    var rewritten = URL_ATTRIBUTE.replace(manifest) { match ->
        val (name, value) = match.destructured
        if (value.isAbsoluteReference()) rewrite(value)?.let { "$name=\"$it\"" } ?: match.value else match.value
    }
    rewritten = BASE_URL.replace(rewritten) { match ->
        val (open, value, close) = match.destructured
        val trimmed = value.trim()
        if (trimmed.isAbsoluteReference()) rewrite(trimmed)?.let { "$open$it$close" } ?: match.value else match.value
    }
    if (redirectedBase != null && !rewritten.contains("<BaseURL")) {
        MPD_OPEN.find(rewritten)?.let { open ->
            rewritten = rewritten.replaceRange(open.range, "${open.value}<BaseURL>$redirectedBase</BaseURL>")
        }
    }
    return rewritten
}

private fun String.isAbsoluteReference(): Boolean =
    startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true) || startsWith("/")

private val URL_ATTRIBUTE = Regex("""\b(media|initialization|sourceURL|index)="([^"]*)"""")
private val BASE_URL = Regex("""(<BaseURL\b[^>]*>)([^<]*)(</BaseURL>)""")
private val MPD_OPEN = Regex("""<MPD\b[^>]*>""")
