package app.katari.sync.ui.arrival.change.named

import app.katari.profiles.core.ProfileStore
import app.katari.tts.api.host.TtsHostActions
import app.katari.tts.api.provider.TtsParameterRange
import eu.kanade.presentation.more.settings.screen.tts.presentation.nearest
import eu.kanade.presentation.more.settings.screen.tts.presentation.pitchOptions
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Text to speech's rate and pitch. The pitch is told by its Settings steps around the default, as the engine's own
 * range isn't known here.
 */
internal fun speechNamedSettings(tts: TtsHostActions, profileStore: ProfileStore): List<NamedSetting> = listOf(
    valueSetting(MR.strings.sync_setting_speech_rate, profileStore.floatPreference(tts.speechRate)) {
        stringResource(MR.strings.sync_arrived_rate, it.toString())
    },
    valueSetting(MR.strings.tts_settings_pitch, profileStore.floatPreference(tts.pitch)) { pitch ->
        pitchOptions(TtsParameterRange(Float.MIN_VALUE, Float.MAX_VALUE, tts.pitch.defaultValue())).nearest(pitch).label
    },
)
