package mihon.translation.runtime

import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.api.TranslationTargetLanguageSelection
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class ProfileTranslationPreferences(
    preferenceStore: PreferenceStore,
    private val defaultEngine: TranslationEngineId,
) {
    val engine: Preference<TranslationEngineId> = preferenceStore.getObjectFromString(
        key = "translation_engine",
        defaultValue = defaultEngine,
        serializer = TranslationEngineId::value,
        deserializer = ::TranslationEngineId,
    )

    val targetLanguage: Preference<TranslationTargetLanguageSelection> = preferenceStore.getObjectFromString(
        key = "translation_target_language",
        defaultValue = TranslationTargetLanguageSelection.Default,
        serializer = ::serializeTargetLanguage,
        deserializer = ::deserializeTargetLanguage,
    )

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
        const val DEFAULT_TARGET_VALUE = "default"
    }
}
