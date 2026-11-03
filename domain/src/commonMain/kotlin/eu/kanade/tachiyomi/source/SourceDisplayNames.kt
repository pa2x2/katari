package eu.kanade.tachiyomi.source

import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.domain.source.model.SourceDisplayInfo

fun SourceDisplayInfo.visualName(): String =
    if (lang.isNotBlank()) "$name (${lang.uppercase()})" else name

fun SourceDisplayInfo.sourceNotInstalledName(): String =
    if (lang.isNotBlank()) visualName() else name

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
