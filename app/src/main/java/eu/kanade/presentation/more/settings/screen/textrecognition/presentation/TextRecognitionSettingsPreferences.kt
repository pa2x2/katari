package eu.kanade.presentation.more.settings.screen.textrecognition.presentation

import androidx.compose.runtime.Composable
import eu.kanade.presentation.more.settings.Preference
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The Text recognition screen's rows. Settings search builds them without values or actions, so both list the same
 * titles and a search hit highlights the row it names.
 */
@Composable
internal fun textRecognitionSettingsPreferences(
    engine: String? = null,
    languages: String? = null,
    storage: String? = null,
    onChooseEngine: (() -> Unit)? = null,
    onChooseLanguages: (() -> Unit)? = null,
    onOpenModels: (() -> Unit)? = null,
    playground: @Composable () -> Unit = {},
): List<Preference> = listOf(
    Preference.PreferenceGroup(
        title = stringResource(MR.strings.text_recognition_settings_group_recognition),
        preferenceItems = listOf(
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.text_recognition_settings_engine),
                subtitle = engine,
                isProfileSpecific = true,
                onClick = onChooseEngine,
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.text_recognition_settings_languages),
                subtitle = languages,
                isProfileSpecific = true,
                onClick = onChooseLanguages,
            ),
        ),
    ),
    Preference.PreferenceGroup(
        title = stringResource(MR.strings.text_recognition_settings_group_storage),
        preferenceItems = listOf(
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.model_artifacts_title),
                subtitle = storage,
                onClick = onOpenModels,
            ),
        ),
    ),
    Preference.PreferenceGroup(
        title = stringResource(MR.strings.text_recognition_settings_try_it),
        preferenceItems = listOf(
            Preference.PreferenceItem.CustomPreference(
                title = stringResource(MR.strings.text_recognition_settings_playground),
                content = playground,
            ),
        ),
    ),
)
