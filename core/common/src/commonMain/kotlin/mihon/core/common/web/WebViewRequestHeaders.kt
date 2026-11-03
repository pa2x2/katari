package mihon.core.common.web

import okhttp3.Headers
import java.util.Locale

/**
 * Whether a web view may send the request header [name] with [value] itself: Chromium fails a load that sets a header
 * it keeps for itself with `net::ERR_INVALID_ARGUMENT`. Based on `IsRequestHeaderSafe` in
 * https://source.chromium.org/chromium/chromium/src/+/main:services/network/public/cpp/header_util.cc
 */
fun isWebViewSafeRequestHeader(name: String, value: String): Boolean {
    val lowercaseName = name.lowercase(Locale.ENGLISH)
    if (lowercaseName in unsafeHeaderNames || lowercaseName.startsWith("proxy-")) return false
    if (lowercaseName == "connection" && value.lowercase(Locale.ENGLISH) == "upgrade") return false
    return true
}

/** These headers less the ones a web view can't send ([isWebViewSafeRequestHeader]). */
fun Map<String, String>.webViewSafeHeaders(): Map<String, String> =
    filter { (name, value) -> isWebViewSafeRequestHeader(name, value) }

/** These headers less the ones a web view can't send, each with the first of its values. */
fun Headers.webViewSafeHeaders(): Map<String, String> = this
    .filter { (name, value) -> isWebViewSafeRequestHeader(name, value) }
    .groupBy(keySelector = { (name, _) -> name }) { (_, value) -> value }
    .mapValues { it.value.getOrNull(0).orEmpty() }

private val unsafeHeaderNames = listOf(
    "content-length", "host", "trailer", "te", "upgrade", "cookie2", "keep-alive", "transfer-encoding", "set-cookie",
    // Renderer is killed if it doesn't match the request initiator
    "origin",
)
