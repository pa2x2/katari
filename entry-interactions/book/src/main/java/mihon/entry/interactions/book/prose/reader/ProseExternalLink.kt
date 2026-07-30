package mihon.entry.interactions.book.prose

import android.net.Uri

internal fun String.toValidatedProseExternalUri(): Uri {
    val uri = Uri.parse(this)
    require(
        uri.scheme.equals("https", ignoreCase = true) ||
            uri.scheme.equals("http", ignoreCase = true),
    ) {
        "Unsupported prose link"
    }
    require(!uri.host.isNullOrBlank()) { "Invalid prose link" }
    return uri.buildUpon()
        .scheme(uri.scheme?.lowercase())
        .build()
}
