package eu.kanade.tachiyomi.ui.main

import android.net.Uri
import eu.kanade.tachiyomi.ui.main.link.AppLink

/** A viewed intent's data as [AppLink] reads it. */
internal class AndroidUriLinkParts(private val uri: Uri) : AppLink.Parts {
    override val text: String get() = uri.toString()
    override val scheme: String? get() = uri.scheme
    override val host: String? get() = uri.host
    override val path: String? get() = uri.path
    override val lastPathSegment: String? get() = uri.pathSegments.lastOrNull()

    // Throws for an opaque URI, which AppLink only asks of an extension store's link, one with a host
    override fun queryParameter(name: String): String? = uri.getQueryParameter(name)
}
