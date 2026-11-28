package eu.kanade.tachiyomi.data.backup.conversion.lnreader.settings

import eu.kanade.tachiyomi.data.backup.models.FloatPreferenceValue
import tachiyomi.i18n.*

/**
 * LNReader's text to speech speed and pitch as Katari's. Katari's speech refuses a value its engine can't take, so one
 * outside what every Katari engine takes is listed rather than written. LNReader's engine and voice are its choice
 * among the device's, by ids Katari doesn't share.
 */
internal fun LnReaderSettings.convertTtsSettings(settings: LnReaderTtsSettings) {
    settings.rate?.let { rate ->
        if (rate in ACCEPTED_BY_EVERY_ENGINE) {
            write("tts_speech_rate", FloatPreferenceValue(rate))
        } else {
            leaveBehind(LnReaderSettingLabels.TTS_SPEED, MR.strings.backup_conversion_lnreader_tts_out_of_range)
        }
    }
    settings.pitch?.let { pitch ->
        if (pitch in ACCEPTED_BY_EVERY_ENGINE) {
            write("tts_pitch", FloatPreferenceValue(pitch))
        } else {
            leaveBehind(LnReaderSettingLabels.TTS_PITCH, MR.strings.backup_conversion_lnreader_tts_out_of_range)
        }
    }
    if (settings.engine != null) {
        leaveBehind(LnReaderSettingLabels.TTS_ENGINE, MR.strings.backup_conversion_lnreader_tts_device_choice)
    }
    if (settings.voice != null) {
        leaveBehind(LnReaderSettingLabels.TTS_VOICE, MR.strings.backup_conversion_lnreader_tts_device_choice)
    }
    leaveBehindHeld(
        settings.autoPageAdvance to LnReaderSettingLabels.TTS_AUTO_PAGE_ADVANCE,
        settings.scrollToTop to LnReaderSettingLabels.TTS_SCROLL_TO_TOP,
    )
}

// Where the rates and pitches of Android's engines (rate 0.1 to 3, pitch 0.5 to 2) and Speech Dispatcher (0.5 to 2)
// overlap
private val ACCEPTED_BY_EVERY_ENGINE = 0.5f..2f
