package mihon.data.extension.model

import android.annotation.SuppressLint
import eu.kanade.tachiyomi.extension.model.ListedDownload
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import mihon.domain.extension.model.ExtensionStore
import mihon.domain.extension.model.ExtensionStoreFormat
import tachiyomi.domain.source.identity.SourceIdScheme
import eu.kanade.tachiyomi.extension.model.Extension as DomainExtension

/**
 * Aniyomi's v2 index. It shares Katari's messages and numbers up to an extension's content warning, then puts the
 * torrent flag where Katari has the sources and the sources where Katari retired a field; a source's message comes
 * before Katari's.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class AniyomiNetworkExtensionStore(
    @ProtoNumber(1) val name: String,
    @ProtoNumber(2) val badgeLabel: String,
    @ProtoNumber(3) val signingKey: String,
    @ProtoNumber(4) val contact: NetworkExtensionStore.Contact,
    @ProtoNumber(101) override val extensionList: ExtensionList?,
    @ProtoNumber(102) val extensionListUrl: String?,
) : NetworkV2ExtensionStore {

    @Serializable
    data class ExtensionList(@ProtoNumber(1) val extensions: List<Extension>) : NetworkExtensionList {

        override val isEmpty: Boolean get() = extensions.isEmpty()

        override fun countFitting(): Int {
            return extensions.count { it.sources.isNotEmpty() && it.extensionLib.isAniyomiLib() }
        }

        override fun toAvailableExtensions(store: ExtensionStore): List<DomainExtension.Available> {
            return extensions.map { it.toAvailableExtension(store) }
        }
    }

    @Serializable
    data class Extension(
        @ProtoNumber(1) val name: String,
        @ProtoNumber(2) val packageName: String,
        @ProtoNumber(3) val resources: NetworkExtensionStore.Resources,
        @ProtoNumber(4) val extensionLib: String,
        @ProtoNumber(5) val versionCode: Long,
        @ProtoNumber(6) val versionName: String,
        @ProtoNumber(7) val contentWarning: NetworkExtensionStore.ContentWarning,
        @ProtoNumber(8) val isTorrent: Boolean = false,
        @ProtoNumber(9) val sources: List<Source>,
    )

    @Serializable
    data class Source(
        @ProtoNumber(1) val id: Long,
        @ProtoNumber(2) val name: String,
        @ProtoNumber(3) val language: String,
        @ProtoNumber(4) val homeUrl: String = "",
        @ProtoNumber(5) val mirrorUrls: List<String> = emptyList(),
        @ProtoNumber(6) val message: String? = null,
    )

    override fun toExtensionStore(indexUrl: String): ExtensionStore {
        return ExtensionStore(
            indexUrl = indexUrl,
            name = name,
            badgeLabel = badgeLabel,
            signingKey = signingKey,
            contact = ExtensionStore.Contact(
                website = contact.website,
                discord = contact.discord,
            ),
            format = ExtensionStoreFormat.V2,
            extensionListUrl = extensionListUrl,
        )
    }
}

private fun AniyomiNetworkExtensionStore.Extension.toAvailableExtension(
    store: ExtensionStore,
): DomainExtension.Available {
    val lang = sources.map { it.language }.toSet()
    return DomainExtension.Available(
        name = name.removePrefix(ANIYOMI_NAME_PREFIX),
        pkgName = packageName,
        download = ListedDownload.Apk(resources.apkUrl),
        iconUrl = resources.iconUrl,
        libVersion = extensionLib.toLibVersionDouble(),
        versionCode = versionCode,
        versionName = versionName,
        lang = if (lang.size == 1) lang.first() else "all",
        contentWarning = contentWarning.toDomainContentWarning(),
        sources = sources.map { source ->
            DomainExtension.Available.Source(
                id = source.id,
                name = source.name,
                lang = source.language,
                baseUrl = source.homeUrl,
                supportedEntryTypes = ANIYOMI_ENTRY_TYPES,
            )
        },
        store = store,
        libVersionName = extensionLib,
        sourceIdScheme = SourceIdScheme.ANIYOMI,
        isTorrent = isTorrent,
    )
}

/** Aniyomi's legacy index names an extension after its apk label. */
internal const val ANIYOMI_NAME_PREFIX = "Aniyomi: "

internal val ANIYOMI_ENTRY_TYPES = setOf(EntryType.ANIME)
