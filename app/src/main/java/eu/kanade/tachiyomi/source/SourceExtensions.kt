package eu.kanade.tachiyomi.source

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.adapter.LegacyMangaSourceAdapter
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import tachiyomi.domain.source.model.SourceDisplayInfo
import tachiyomi.domain.source.model.StubSource
import tachiyomi.domain.source.model.UnifiedStubSource
import tachiyomi.domain.source.service.CatalogSource
import tachiyomi.source.local.LocalSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

fun Source.getDisplayNameForEntryInfo(): String {
    return SourceDisplayInfo(
        id = id,
        name = name,
        lang = lang,
        isMissing = this is StubSource,
    ).getDisplayNameForEntryInfo()
}

fun SourceDisplayInfo.getDisplayNameForEntryInfo(): String {
    val preferences = Injekt.get<SourcePreferences>()
    val enabledLanguages = preferences.enabledLanguages.get()
        .filterNot { it in listOf("all", "other") }
    val hasOneActiveLanguages = enabledLanguages.size == 1
    val isInEnabledLanguages = lang in enabledLanguages
    return when {
        // For edge cases where user disables a source they got manga of in their library.
        hasOneActiveLanguages && !isInEnabledLanguages -> visualName()
        // Hide the language tag when only one language is used.
        hasOneActiveLanguages && isInEnabledLanguages -> name
        else -> visualName()
    }
}

fun SourceDisplayInfo.visualName(): String =
    if (lang.isNotBlank()) "$name (${lang.uppercase()})" else name

fun SourceDisplayInfo.sourceNotInstalledName(): String =
    if (lang.isNotBlank()) visualName() else name

fun Source?.isLocalOrStub(): Boolean = this == null || id == LocalSource.ID || this is StubSource

fun UnifiedSource?.isLocalOrStub(): Boolean = this == null || id == LocalSource.ID || this is UnifiedStubSource

/**
 * Returns a concrete legacy/local [Source] only for compatibility paths that
 * still require the old manga source API. Entry-first display and metadata
 * flows should use SourceManager display metadata instead.
 */
fun UnifiedSource.toLegacySourceForCompatibility(): Source? =
    when (this) {
        is LegacyMangaSourceAdapter -> source
        else -> null
    }

fun UnifiedSource.toCatalogSource(): CatalogSource? =
    (this as? EntryCatalogueSource)?.let(CatalogSource::Mixed)
