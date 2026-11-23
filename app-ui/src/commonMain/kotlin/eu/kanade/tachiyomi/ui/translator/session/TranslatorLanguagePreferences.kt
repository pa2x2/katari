package eu.kanade.tachiyomi.ui.translator.session

import app.katari.translation.api.preference.getSourceLanguageSelection
import app.katari.translation.api.preference.getTargetLanguageSelection
import app.katari.translation.api.request.TranslationSourceLanguageSelection
import app.katari.translation.api.request.TranslationTargetLanguageSelection
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/**
 * The last languages used in the Translator tab, so it reopens with them. They belong to the tab alone: readers and
 * the profile's default target never read or write them.
 */
class TranslatorLanguagePreferences(preferenceStore: PreferenceStore) {
    val sourceLanguage: Preference<TranslationSourceLanguageSelection> =
        preferenceStore.getSourceLanguageSelection("translator_source_language")

    val targetLanguage: Preference<TranslationTargetLanguageSelection> =
        preferenceStore.getTargetLanguageSelection("translator_target_language")
}
