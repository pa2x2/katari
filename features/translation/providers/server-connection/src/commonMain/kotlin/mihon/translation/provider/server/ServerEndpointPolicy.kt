package mihon.translation.provider.server

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Accepts only server endpoints that keep text and keys private in transit: HTTPS, or HTTP on a loopback address.
 * Endpoints may carry no credentials, query, or fragment, and are normalized to end with a slash.
 */
object ServerEndpointPolicy {
    fun validate(value: String): HttpUrl? {
        val endpoint = value.trim().toHttpUrlOrNull() ?: return null
        if (endpoint.username.isNotEmpty() || endpoint.password.isNotEmpty()) return null
        if (endpoint.query != null || endpoint.fragment != null) return null
        val loopback = endpoint.host == "localhost" ||
            endpoint.host == "127.0.0.1" ||
            endpoint.host == "::1"
        if (endpoint.scheme != "https" && !(endpoint.scheme == "http" && loopback)) return null
        return endpoint.newBuilder()
            .encodedPath(endpoint.encodedPath.trimEnd('/') + "/")
            .build()
    }
}
