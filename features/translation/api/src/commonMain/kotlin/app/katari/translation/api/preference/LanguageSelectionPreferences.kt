package app.katari.translation.api.preference

import app.katari.language.api.tag.LanguageTag
import app.katari.translation.api.request.TranslationSourceLanguageSelection
import app.katari.translation.api.request.TranslationTargetLanguageSelection
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/** A source language as stores keep it: "automatic", or the language's tag. */
fun PreferenceStore.getSourceLanguageSelection(key: String): Preference<TranslationSourceLanguageSelection> =
    getObjectFromString(
        key = key,
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

/** A target language as stores keep it: "default" for the profile's or app's language, or the language's tag. */
fun PreferenceStore.getTargetLanguageSelection(key: String): Preference<TranslationTargetLanguageSelection> =
    getObjectFromString(
        key = key,
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

/** Languages as stores keep them, in order and each once: their tags, comma-separated. */
fun PreferenceStore.getLanguageTags(key: String): Preference<List<LanguageTag>> = getObjectFromString(
    key = key,
    defaultValue = emptyList(),
    serializer = { languages -> languages.joinToString(LANGUAGE_SEPARATOR) { it.value } },
    deserializer = { raw -> raw.split(LANGUAGE_SEPARATOR).mapNotNull(LanguageTag::parse).distinct() },
)

private const val AUTOMATIC_SOURCE_VALUE = "automatic"
private const val DEFAULT_TARGET_VALUE = "default"
private const val LANGUAGE_SEPARATOR = ","
