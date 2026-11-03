package eu.kanade.tachiyomi.ui.main.link

import tachiyomi.domain.storage.DocumentUri

/**
 * What a link handed to the app from outside asks it to open, as Android's main activity reads a viewed link: a
 * backup to restore, an entry, or an extension store to add. Each platform decides which links reach the app (the
 * Android manifest's intent filters, the desktop's launch arguments) and parses them with its own URI type; [of] reads
 * the parts it gets the same way on every platform.
 */
sealed interface AppLink {
    data class RestoreBackup(val backup: DocumentUri) : AppLink

    data class Entry(val entryId: Long) : AppLink

    data class AddExtensionStore(val indexUrl: String) : AppLink

    /** The parts of a link [of] reads, as the platform's URI type parses them. */
    interface Parts {
        val text: String
        val scheme: String?
        val host: String?

        /** The path, decoded; null for an opaque link, such as `app.katari:entry/1`. */
        val path: String?

        /** The last non-empty segment of the path, decoded. */
        val lastPathSegment: String?

        /** The decoded value of the query's first parameter [name], or null when the query has none. */
        fun queryParameter(name: String): String?
    }

    companion object {
        /** What [link] opens, or null for a link the app doesn't open. The first rule a link matches decides. */
        fun of(link: Parts): AppLink? = when {
            link.text.endsWith(".tachibk") -> RestoreBackup(DocumentUri(link.text))
            link.path?.startsWith("/entry/") == true -> link.lastPathSegment?.toLongOrNull()?.let(::Entry)
            link.isExtensionStoreLink() -> link.queryParameter("url")?.let(::AddExtensionStore)
            else -> null
        }

        private fun Parts.isExtensionStoreLink(): Boolean =
            (scheme == "tachiyomi" && host == "add-repo") || (scheme == "katari" && host == "extension-store")
    }
}
