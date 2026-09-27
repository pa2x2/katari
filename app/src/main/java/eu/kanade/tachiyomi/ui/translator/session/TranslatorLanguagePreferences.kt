package eu.kanade.tachiyomi.ui.translator.session

import mihon.language.api.tag.LanguageTag
import mihon.translation.api.request.TranslationSourceLanguageSelection
import mihon.translation.api.request.TranslationTargetLanguageSelection
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/**
 * The last languages used in the Translator tab, so it reopens with them. They belong to the tab alone: readers and
 * the profile's default target never read or write them.
 */
class TranslatorLanguagePreferences(preferenceStore: PreferenceStore) {
    val sourceLanguage: Preference<TranslationSourceLanguageSelection> = preferenceStore.getObjectFromString(
        key = "translator_source_language",
        defaultValue = TranslationSourceLanguageSelection.Automatic,
        serializer = { selection ->
            when (selection) {
                TranslationSourceLanguageSelection.Automatic -> AUTOMATIC_SOURCE_VALUE
                is TranslationSourceLanguageSelection.Explicit -> selection.language.value
            }
        },
        deserializer = { value ->
            value.takeUnless { it == AUTOMATIC_SOURCE_VALUE }
                ?.let(LanguageTag::parse)
                ?.let(TranslationSourceLanguageSelection::Explicit)
                ?: TranslationSourceLanguageSelection.Automatic
        },
    )

    val targetLanguage: Preference<TranslationTargetLanguageSelection> = preferenceStore.getObjectFromString(
        key = "translator_target_language",
        defaultValue = TranslationTargetLanguageSelection.Default,
        serializer = { selection ->
            when (selection) {
                TranslationTargetLanguageSelection.Default -> DEFAULT_TARGET_VALUE
                is TranslationTargetLanguageSelection.Explicit -> selection.language.value
            }
        },
        deserializer = { value ->
            value.takeUnless { it == DEFAULT_TARGET_VALUE }
                ?.let(LanguageTag::parse)
                ?.let(TranslationTargetLanguageSelection::Explicit)
                ?: TranslationTargetLanguageSelection.Default
        },
    )

    private companion object {
        const val AUTOMATIC_SOURCE_VALUE = "automatic"
        const val DEFAULT_TARGET_VALUE = "default"
    }
}
