package app.katari.sync.ui.arrival.change.named

import app.katari.profiles.core.ProfileStore
import app.katari.translation.api.host.TranslationHostActions
import app.katari.translation.api.preference.getLanguageTags
import app.katari.translation.api.preference.getTargetLanguageSelection
import app.katari.translation.api.request.TranslationSourceLanguageSelection
import app.katari.translation.api.request.TranslationTargetLanguageSelection
import app.katari.translation.ui.picker.language.displayName
import eu.kanade.tachiyomi.ui.translator.session.TranslatorLanguagePreferences
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Translation's target and recent languages, and the languages the Translator tab reopens with. */
internal fun translationNamedSettings(
    translation: TranslationHostActions,
    profileStore: ProfileStore,
): List<NamedSetting> {
    val targetKey = profileStore.preferenceKey(translation.defaultTargetLanguage)
    val recentKey = profileStore.preferenceKey(translation.recentLanguages)
    return listOf(
        valueSetting(MR.strings.translation_settings_target, { it.getTargetLanguageSelection(targetKey) }) { target ->
            when (target) {
                TranslationTargetLanguageSelection.Default ->
                    stringResource(MR.strings.translation_settings_target_app_language)
                is TranslationTargetLanguageSelection.Explicit -> target.language.displayName()
            }
        },
        orderSetting(MR.strings.sync_setting_recent_languages, { it.getLanguageTags(recentKey) }) { it.displayName() },
        valueSetting(MR.strings.sync_setting_translator_source, {
            TranslatorLanguagePreferences(it).sourceLanguage
        }) { source ->
            when (source) {
                TranslationSourceLanguageSelection.Automatic -> stringResource(MR.strings.translator_detect_language)
                is TranslationSourceLanguageSelection.Explicit -> source.language.displayName()
            }
        },
        valueSetting(MR.strings.sync_setting_translator_target, {
            TranslatorLanguagePreferences(it).targetLanguage
        }) { target ->
            when (target) {
                TranslationTargetLanguageSelection.Default -> stringResource(MR.strings.translator_profile_default)
                is TranslationTargetLanguageSelection.Explicit -> target.language.displayName()
            }
        },
    )
}
