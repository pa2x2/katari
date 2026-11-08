package mihon.extension.compat.webkit

import android.webkit.WebViewClient

/**
 * The `WebViewClient.ERROR_*` code for Chromium's network error [netError], after the mapping of Android's web view
 * (Chromium's `ErrorCodeConversionHelper`). Chromium's codes are those of its `net/base/net_error_list.h`.
 */
internal fun webViewErrorCode(netError: Int): Int = when (netError) {
    IO_PENDING, NETWORK_IO_SUSPENDED -> WebViewClient.ERROR_IO
    TIMED_OUT, CONNECTION_TIMED_OUT -> WebViewClient.ERROR_TIMEOUT
    FILE_TOO_BIG -> WebViewClient.ERROR_FILE
    UPLOAD_FILE_CHANGED -> WebViewClient.ERROR_FILE_NOT_FOUND
    INSUFFICIENT_RESOURCES, OUT_OF_MEMORY -> WebViewClient.ERROR_TOO_MANY_REQUESTS
    in CONNECT_ERRORS -> WebViewClient.ERROR_CONNECT
    in HOST_LOOKUP_ERRORS -> WebViewClient.ERROR_HOST_LOOKUP
    in PROXY_ERRORS -> WebViewClient.ERROR_PROXY_AUTHENTICATION
    in SSL_ERRORS, in CERTIFICATE_ERRORS -> WebViewClient.ERROR_FAILED_SSL_HANDSHAKE
    INVALID_URL -> WebViewClient.ERROR_BAD_URL
    DISALLOWED_URL_SCHEME, UNKNOWN_URL_SCHEME -> WebViewClient.ERROR_UNSUPPORTED_SCHEME
    TOO_MANY_REDIRECTS -> WebViewClient.ERROR_REDIRECT_LOOP
    UNSUPPORTED_AUTH_SCHEME -> WebViewClient.ERROR_UNSUPPORTED_AUTH_SCHEME
    INVALID_AUTH_CREDENTIALS, MISSING_AUTH_CREDENTIALS, MISCONFIGURED_AUTH_ENVIRONMENT ->
        WebViewClient.ERROR_AUTHENTICATION
    else -> WebViewClient.ERROR_UNKNOWN
}

private const val IO_PENDING = -1
private const val TIMED_OUT = -7
private const val FILE_TOO_BIG = -8
private const val INSUFFICIENT_RESOURCES = -12
private const val OUT_OF_MEMORY = -13
private const val UPLOAD_FILE_CHANGED = -14
private const val CONNECTION_TIMED_OUT = -118
private const val INVALID_URL = -300
private const val DISALLOWED_URL_SCHEME = -301
private const val UNKNOWN_URL_SCHEME = -302
private const val TOO_MANY_REDIRECTS = -310
private const val NETWORK_IO_SUSPENDED = -331
private const val INVALID_AUTH_CREDENTIALS = -338
private const val UNSUPPORTED_AUTH_SCHEME = -339
private const val MISSING_AUTH_CREDENTIALS = -341
private const val MISCONFIGURED_AUTH_ENVIRONMENT = -343

// Closed, reset, refused, aborted, failed, and a socket that isn't connected
private val CONNECT_ERRORS = setOf(-100, -101, -102, -103, -104, -15)

// Internet disconnected, an address invalid or unreachable, a name not resolved
private val HOST_LOOKUP_ERRORS = setOf(-106, -108, -109, -105, -137)

// Proxy authentication unsupported or requested, a proxy connection failed, unexpected proxy authentication
private val PROXY_ERRORS = setOf(-115, -127, -130, -323)

// The TLS handshake's own failures
private val SSL_ERRORS = setOf(-107, -110, -111, -112, -113, -114, -116, -117, -123, -125, -126, -129, -134, -135)

private val CERTIFICATE_ERRORS = -299..-200
