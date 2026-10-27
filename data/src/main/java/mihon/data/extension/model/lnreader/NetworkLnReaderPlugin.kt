package mihon.data.extension.model.lnreader

import android.annotation.SuppressLint
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.ListedDownload
import eu.kanade.tachiyomi.extension.model.fileExtensionPkgName
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import mihon.data.extension.model.toLibVersionDouble
import mihon.domain.extension.model.ContentWarning
import mihon.domain.extension.model.ExtensionStore
import tachiyomi.domain.source.identity.SourceIdScheme
import tachiyomi.domain.source.identity.lnReaderExternalId

/**
 * An entry of an LNReader `plugins.min.json`. Every field LNReader's own index writes is required, and [version] must
 * be dotted numbers, so an entry of another family's array never reads as one.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class NetworkLnReaderPlugin(
    val id: String,
    val name: String,
    val site: String,
    val lang: String,
    val version: String,
    val url: String,
    val iconUrl: String,
    val customCSS: String? = null,
    /** Written by Nekori's index for plugins that need its app's APIs. */
    val isNekoriPlugin: Boolean? = null,
    val minApiVersion: JsonElement? = null,
) {
    init {
        require(id.isNotBlank()) { "it has an empty id" }
        require(VERSION.matches(version)) { "its version $version isn't dotted numbers" }
    }

    /**
     * The `lnreader-plugins` version it's published for, from the `/plugins/v<version>/` its URL is on; empty when the
     * URL doesn't say, or when it needs Nekori's app, both of which Katari can't run.
     */
    val libVersionName: String
        get() = if (isNekoriPlugin == true ||
            minApiVersion != null
        ) {
            ""
        } else {
            PUBLISHED_FOR.find(url)?.groupValues?.get(1).orEmpty()
        }

    fun toAvailableExtension(store: ExtensionStore): Extension.Available {
        val lang = lnReaderLanguageCode(lang)
        return Extension.Available(
            name = name,
            pkgName = fileExtensionPkgName(SourceIdScheme.LNREADER, id),
            versionName = version,
            versionCode = 0,
            libVersion = libVersionName.takeIf { it.isNotEmpty() }?.toLibVersionDouble() ?: 0.0,
            lang = lang,
            contentWarning = ContentWarning.NOT_RATED,
            sources = listOf(
                Extension.Available.Source(
                    id = lnReaderExternalId(id),
                    lang = lang,
                    name = name,
                    baseUrl = site,
                    supportedEntryTypes = setOf(EntryType.BOOK),
                ),
            ),
            download = ListedDownload.File(
                url = url,
                companions = customCSS?.let { mapOf(CUSTOM_CSS to it) }.orEmpty(),
            ),
            iconUrl = iconUrl,
            store = store,
            libVersionName = libVersionName,
            sourceIdScheme = SourceIdScheme.LNREADER,
            isTorrent = false,
        )
    }

    companion object {
        /** The name the adapter reads a plugin's style sheet by, as LNReader installs it. */
        const val CUSTOM_CSS = "custom.css"

        private val VERSION = Regex("\\d+(\\.\\d+)*")
        private val PUBLISHED_FOR = Regex("/plugins/v(\\d+\\.\\d+\\.\\d+)/")
    }
}
