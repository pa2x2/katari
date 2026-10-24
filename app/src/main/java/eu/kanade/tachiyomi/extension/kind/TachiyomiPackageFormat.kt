package eu.kanade.tachiyomi.extension.kind

import android.os.Bundle
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.source.adapter.asUnifiedSource
import eu.kanade.tachiyomi.source.entry.EntrySourceFactory
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import mihon.domain.extension.model.ContentWarning

/*
 * What packages declaring `tachiyomi.extension` share, whether built against the Tachiyomi extensions-lib or Katari's
 * Entry SDK: the manifest entries and the classes their sources may be.
 */

internal const val TACHIYOMI_EXTENSION_FEATURE = "tachiyomi.extension"

private const val METADATA_SOURCE_CLASS = "tachiyomi.extension.class"
private const val METADATA_SOURCE_FACTORY = "tachiyomi.extension.factory"
private const val METADATA_NSFW = "tachiyomi.extension.nsfw"

private const val METADATA_NAME = "tachiyomix.name"
private const val METADATA_EXTENSION_LIB = "tachiyomix.extensionLib"
private const val METADATA_CONTENT_WARNING = "tachiyomix.contentWarning"

internal fun describeTachiyomiPackage(
    packageName: String,
    versionName: String,
    metaData: Bundle,
    label: () -> String,
    runsLib: (String) -> Boolean,
): ExtensionDescription {
    @Suppress("DEPRECATION")
    val libVersionName = extensionLibValue(metaData.get(METADATA_EXTENSION_LIB))
        ?: versionName.substringBeforeLast('.')
    return ExtensionDescription(
        name = (metaData.getString(METADATA_NAME) ?: label())
            .removePrefix("Tachiyomi: ")
            .removePrefix("Katari: ")
            .removePrefix("K-Mihon: "),
        contentWarning = when {
            metaData.containsKey(
                METADATA_CONTENT_WARNING,
            ) -> contentWarningOf(metaData.getInt(METADATA_CONTENT_WARNING))
            metaData.getInt(METADATA_NSFW) == 1 -> ContentWarning.NSFW
            else -> ContentWarning.SAFE
        },
        libVersionName = libVersionName,
        libVersion = LibVersion.parse(libVersionName)?.toDouble(),
        sourceClassNames = sourceClassNames(packageName, metaData.getString(METADATA_SOURCE_CLASS)),
        factory = metaData.getString(METADATA_SOURCE_FACTORY),
        refusal = Extension.NotLoaded.Reason.UnsupportedLibVersion.takeUnless { runsLib(libVersionName) },
    )
}

/** The content warning a manifest's level stands for: 1 mixed, 2 NSFW, anything else safe. */
internal fun contentWarningOf(level: Int): ContentWarning {
    return when (level) {
        1 -> ContentWarning.MIXED
        2 -> ContentWarning.NSFW
        else -> ContentWarning.SAFE
    }
}

/** The `;`-separated class names of a manifest entry, a leading `.` meaning one in [packageName]. */
internal fun sourceClassNames(packageName: String, declared: String?): List<String> {
    if (declared.isNullOrBlank()) return emptyList()
    return declared.split(";").map {
        val sourceClass = it.trim()
        if (sourceClass.startsWith(".")) packageName + sourceClass else sourceClass
    }
}

/** Instantiates a `tachiyomi.extension` package's source classes, legacy sources wrapped as Entry SDK ones. */
internal fun createTachiyomiSources(classNames: List<String>, classLoader: ClassLoader): List<UnifiedSource> {
    return classNames.flatMap { sourceClass ->
        when (val obj = Class.forName(sourceClass, false, classLoader).getDeclaredConstructor().newInstance()) {
            is UnifiedSource -> listOf(obj)
            is EntrySourceFactory -> obj.createSources()
            is Source -> listOf(obj.asUnifiedSource())
            is SourceFactory -> obj.createSources().map { it.asUnifiedSource() }
            else -> throw Exception("Unknown source class type: ${obj.javaClass}")
        }
    }
}
