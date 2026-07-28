package mihon.translation.runtime

import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationEngineSelection
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.api.TranslationTargetLanguageSelection
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class ProfileTranslationPreferences(
    preferenceStore: PreferenceStore,
) {
    val engineSelection: Preference<TranslationEngineSelection> = preferenceStore.getObjectFromString(
        key = "translation_engine",
        defaultValue = TranslationEngineSelection.Automatic,
        serializer = ::serializeEngineSelection,
        deserializer = ::deserializeEngineSelection,
    )

    val targetLanguage: Preference<TranslationTargetLanguageSelection> = preferenceStore.getObjectFromString(
        key = "translation_target_language",
        defaultValue = TranslationTargetLanguageSelection.Default,
        serializer = ::serializeTargetLanguage,
        deserializer = ::deserializeTargetLanguage,
    )

    /**
     * Reserved for future reader adapters. No settings row or runtime behavior may be exposed before an adapter ships.
     */
    val automaticSelectionTranslationEnabled: Preference<Boolean> = preferenceStore.getBoolean(
        key = "translation_automatic_selection_enabled",
        defaultValue = false,
    )

    private fun serializeEngineSelection(selection: TranslationEngineSelection): String {
        return when (selection) {
            TranslationEngineSelection.Automatic -> AUTOMATIC_VALUE
            is TranslationEngineSelection.Explicit -> selection.engine.value
        }
    }

    private fun deserializeEngineSelection(value: String): TranslationEngineSelection {
        if (value == AUTOMATIC_VALUE) return TranslationEngineSelection.Automatic
        return runCatching {
            TranslationEngineSelection.Explicit(TranslationEngineId(value))
        }.getOrDefault(TranslationEngineSelection.Automatic)
    }

    private fun serializeTargetLanguage(selection: TranslationTargetLanguageSelection): String {
        return when (selection) {
            TranslationTargetLanguageSelection.Default -> DEFAULT_TARGET_VALUE
            is TranslationTargetLanguageSelection.Explicit -> selection.language.value
        }
    }

    private fun deserializeTargetLanguage(value: String): TranslationTargetLanguageSelection {
        if (value == DEFAULT_TARGET_VALUE) return TranslationTargetLanguageSelection.Default
        return TranslationLanguageTag.parse(value)
            ?.let(TranslationTargetLanguageSelection::Explicit)
            ?: TranslationTargetLanguageSelection.Default
    }

    private companion object {
        const val AUTOMATIC_VALUE = "automatic"
        const val DEFAULT_TARGET_VALUE = "default"
    }
}
