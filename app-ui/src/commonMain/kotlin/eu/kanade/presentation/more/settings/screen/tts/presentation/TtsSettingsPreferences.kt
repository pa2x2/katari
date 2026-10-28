package eu.kanade.presentation.more.settings.screen.tts.presentation

import androidx.compose.runtime.Composable
import eu.kanade.presentation.more.settings.Preference
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The Text to speech screen's rows. Settings search builds them without values or actions, so both list the same
 * titles and a search hit highlights the row it names. [pitch] is null when the engine cannot change pitch.
 */
@Composable
internal fun ttsSettingsPreferences(
    engine: String? = null,
    defaultVoice: String? = null,
    voiceOverrides: String? = null,
    onChooseEngine: (() -> Unit)? = null,
    onChooseDefaultVoice: (() -> Unit)? = null,
    onChooseVoiceOverrides: (() -> Unit)? = null,
    pitch: (@Composable () -> Unit)? = {},
    preview: @Composable () -> Unit = {},
): List<Preference> = listOf(
    Preference.PreferenceGroup(
        title = stringResource(MR.strings.tts_settings_speech),
        preferenceItems = listOfNotNull(
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.tts_settings_engine),
                subtitle = engine,
                onClick = onChooseEngine,
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.tts_settings_default_voice),
                subtitle = defaultVoice,
                onClick = onChooseDefaultVoice,
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.tts_settings_language_overrides),
                subtitle = voiceOverrides,
                onClick = onChooseVoiceOverrides,
            ),
            pitch?.let {
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.tts_settings_pitch),
                    content = it,
                )
            },
        ),
    ),
    Preference.PreferenceGroup(
        title = stringResource(MR.strings.tts_settings_preview),
        preferenceItems = listOf(
            Preference.PreferenceItem.CustomPreference(
                title = stringResource(MR.strings.tts_settings_playground),
                content = preview,
            ),
        ),
    ),
)
