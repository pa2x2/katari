package mihon.video.engine.dash

import okhttp3.HttpUrl
import org.jsoup.nodes.Element

/**
 * [text], a manifest fetched from [url] after redirects, with the URLs it names sent through [route], a proxy. Every
 * `BaseURL` is resolved against the levels above it and routed as an absolute URL, and a manifest without one at its
 * top gets one for its own folder, so relative segment references resolve against where the manifest really is,
 * whatever URL the player asked for. Absolute and root-relative segment references and `Location` are routed too;
 * relative ones stay, under a routed base. A URL [route] declines (null) stays as written.
 */
fun routeDashManifest(text: String, url: HttpUrl, route: (HttpUrl) -> String?): String {
    val mpd = parseMpd(text)
    routeLevel(mpd, url, route)
    if (mpd.firstChildNamed("BaseURL") == null) {
        val folder = url.resolve(".") ?: url
        route(folder)?.let { mpd.prependElement("${mpd.namespacePrefix}BaseURL").text(it) }
    }
    return checkNotNull(mpd.ownerDocument()).toXml()
}

/** Routes the `BaseURL`s of [element], a level of the manifest, and everything below it. */
private fun routeLevel(element: Element, parentBase: HttpUrl, route: (HttpUrl) -> String?) {
    val base = element.firstChildNamed("BaseURL")?.text()?.trim()?.ifEmpty { null }?.let(parentBase::resolve)
        ?: parentBase
    element.childrenNamed("BaseURL").forEach { baseUrl ->
        parentBase.resolve(baseUrl.text().trim())?.let(route)?.let { baseUrl.text(it) }
    }
    element.children().forEach { child ->
        when (child.localName) {
            in LEVELS -> routeLevel(child, base, route)
            in ADDRESSING_ELEMENTS -> routeAddressing(child, base, route)
            "Location" -> parentBase.resolve(child.text().trim())?.let(route)?.let { child.text(it) }
        }
    }
}

private fun routeAddressing(addressing: Element, base: HttpUrl, route: (HttpUrl) -> String?) {
    for (element in addressing.allElements) {
        for (name in URL_ATTRIBUTES) {
            val reference = element.attrOrNull(name)?.takeIf(String::isAbsoluteReference) ?: continue
            base.resolve(reference)?.let(route)?.let { element.attr(name, it) }
        }
    }
}

private fun String.isAbsoluteReference(): Boolean =
    startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true) || startsWith("/")

private val LEVELS = setOf("Period", "AdaptationSet", "Representation", "SubRepresentation")
private val URL_ATTRIBUTES = listOf("media", "initialization", "index", "sourceURL")
