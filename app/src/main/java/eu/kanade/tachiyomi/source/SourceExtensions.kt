package eu.kanade.tachiyomi.source

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import tachiyomi.domain.source.model.SourceDisplayInfo
import tachiyomi.domain.source.model.StubSource
import tachiyomi.domain.source.model.UnifiedStubSource
import tachiyomi.source.local.LocalSource

fun Source.getDisplayNameForEntryInfo(preferences: SourcePreferences): String {
    return SourceDisplayInfo(
        id = id,
        name = name,
        lang = lang,
        isMissing = this is StubSource,
        hasKnownName = name.isNotBlank(),
    ).getDisplayNameForEntryInfo(preferences)
}

fun SourceDisplayInfo.getDisplayNameForEntryInfo(preferences: SourcePreferences): String {
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
