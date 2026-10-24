package mihon.data.extension.model

import android.annotation.SuppressLint
import eu.kanade.tachiyomi.extension.model.Extension
import kotlinx.serialization.Serializable
import mihon.domain.extension.model.ContentWarning
import mihon.domain.extension.model.ExtensionStore
import tachiyomi.domain.source.identity.SourceIdScheme

/** An entry of a legacy `index.min.json`, which Tachiyomi and Aniyomi stores write alike. */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class NetworkLegacyExtension(
    val name: String,
    val pkg: String,
    val apk: String,
    val lang: String,
    val code: Long,
    val version: String,
    val nsfw: Int,
    /** Only Aniyomi stores write it, and not all of them. */
    val torrent: Int = 0,
    val sources: List<Source>?,
) {
    @Serializable
    data class Source(
        val id: Long,
        val lang: String,
        val name: String,
        val baseUrl: String,
        val supportedEntryTypes: List<String> = emptyList(),
    )

    val libVersionName: String get() = version.substringBeforeLast('.')

    fun toAvailableExtension(store: ExtensionStore, storeBaseUrl: String, isAniyomi: Boolean): Extension.Available {
        val entryTypes = if (isAniyomi) ANIYOMI_ENTRY_TYPES else version.legacyMangaEntryTypes()
        return Extension.Available(
            name = if (isAniyomi) name.removePrefix(ANIYOMI_NAME_PREFIX) else name.substringAfter("Tachiyomi: "),
            pkgName = pkg,
            apkUrl = "$storeBaseUrl/apk/$apk",
            iconUrl = "$storeBaseUrl/icon/$pkg.png",
            libVersion = libVersionName.toLibVersionDouble(),
            versionCode = code,
            versionName = version,
            lang = lang,
            contentWarning = if (nsfw == 1) ContentWarning.NSFW else ContentWarning.SAFE,
            sources = if (sources.isNullOrEmpty()) {
                listOf(
                    Extension.Available.Source(
                        id = 0,
                        name = name,
                        lang = lang,
                        baseUrl = "",
                        supportedEntryTypes = entryTypes,
                    ),
                )
            } else {
                sources.map { source ->
                    Extension.Available.Source(
                        id = source.id,
                        name = source.name,
                        lang = source.lang,
                        baseUrl = source.baseUrl,
                        supportedEntryTypes = source.supportedEntryTypes.toSupportedEntryTypes()
                            ?: entryTypes,
                    )
                }
            },
            store = store,
            libVersionName = libVersionName,
            sourceIdScheme = if (isAniyomi) SourceIdScheme.ANIYOMI else SourceIdScheme.TACHIYOMI,
            isTorrent = torrent == 1,
        )
    }
}

/**
 * The legacy format is the same for Tachiyomi and Aniyomi stores, and not every Aniyomi store writes `torrent`, so the
 * extension libraries tell whose index it is. One that mixes both forms fails to load rather than being guessed at.
 */
fun List<NetworkLegacyExtension>.toAvailableExtensions(
    store: ExtensionStore,
    storeBaseUrl: String,
): List<Extension.Available> {
    val isAniyomi = isNotEmpty() && all { it.libVersionName.isAniyomiLib() }
    require(isAniyomi || none { it.libVersionName.isAniyomiLib() }) {
        "The index lists both Tachiyomi and Aniyomi extensions"
    }
    return map { it.toAvailableExtension(store, storeBaseUrl, isAniyomi) }
}
