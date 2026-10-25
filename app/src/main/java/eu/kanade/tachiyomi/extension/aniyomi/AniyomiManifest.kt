package eu.kanade.tachiyomi.extension.aniyomi

import android.os.Bundle
import eu.kanade.tachiyomi.extension.kind.ExtensionDescription
import eu.kanade.tachiyomi.extension.kind.ExtensionKind
import eu.kanade.tachiyomi.extension.kind.contentWarningOf
import eu.kanade.tachiyomi.extension.kind.extensionLibValue
import eu.kanade.tachiyomi.extension.kind.sourceClassNames
import eu.kanade.tachiyomi.extension.model.Extension
import mihon.domain.extension.model.ContentWarning

/*
 * How an Aniyomi extension describes itself. Aniyomi reads the `aniyomix.*` keys, which catalogues built with its
 * lib 17 plugin write, but most catalogues, Yūzōnō's included, still write the `tachiyomix.*` ones, so both are read,
 * Aniyomi's first. A package with neither falls back on its label, version name and the NSFW flag, as Aniyomi does.
 */

internal const val ANIYOMI_EXTENSION_FEATURE = "tachiyomi.animeextension"

private const val METADATA_SOURCE_CLASS = "tachiyomi.animeextension.class"
private const val METADATA_SOURCE_FACTORY = "tachiyomi.animeextension.factory"
private const val METADATA_NSFW = "tachiyomi.animeextension.nsfw"
private const val METADATA_TORRENT = "tachiyomi.animeextension.torrent"

private const val METADATA_NAME = "aniyomix.name"
private const val METADATA_EXTENSION_LIB = "aniyomix.extensionLib"
private const val METADATA_CONTENT_WARNING = "aniyomix.contentWarning"
private const val METADATA_IS_TORRENT = "aniyomix.torrent"

private const val TACHIYOMI_METADATA_NAME = "tachiyomix.name"
private const val TACHIYOMI_METADATA_EXTENSION_LIB = "tachiyomix.extensionLib"
private const val TACHIYOMI_METADATA_CONTENT_WARNING = "tachiyomix.contentWarning"

internal fun describeAniyomiPackage(
    packageName: String,
    versionName: String,
    metaData: Bundle,
    label: () -> String,
    kind: ExtensionKind,
): ExtensionDescription {
    @Suppress("DEPRECATION")
    val libVersionName = (
        extensionLibValue(metaData.get(METADATA_EXTENSION_LIB))
            ?: extensionLibValue(metaData.get(TACHIYOMI_METADATA_EXTENSION_LIB))
        )
        ?: versionName.substringBeforeLast('.')
    val contentWarningKey = listOf(METADATA_CONTENT_WARNING, TACHIYOMI_METADATA_CONTENT_WARNING)
        .firstOrNull(metaData::containsKey)
    val isTorrent = metaData.getBoolean(METADATA_IS_TORRENT) || metaData.getInt(METADATA_TORRENT) == 1
    return ExtensionDescription(
        name = metaData.getString(METADATA_NAME)
            ?: metaData.getString(TACHIYOMI_METADATA_NAME)
            ?: label().removePrefix("Aniyomi: "),
        contentWarning = when {
            contentWarningKey != null -> contentWarningOf(metaData.getInt(contentWarningKey))
            metaData.getInt(METADATA_NSFW) == 1 -> ContentWarning.NSFW
            else -> ContentWarning.SAFE
        },
        libVersionName = libVersionName,
        libVersion = libVersionName.toDoubleOrNull(),
        sourceClassNames = sourceClassNames(packageName, metaData.getString(METADATA_SOURCE_CLASS)),
        factory = metaData.getString(METADATA_SOURCE_FACTORY),
        refusal = when {
            isTorrent -> Extension.NotLoaded.Reason.Torrent
            !kind.runsLib(libVersionName) ->
                Extension.NotLoaded.Reason.UnsupportedLibVersion(libVersionName, kind.providerKind)
            else -> null
        },
    )
}
