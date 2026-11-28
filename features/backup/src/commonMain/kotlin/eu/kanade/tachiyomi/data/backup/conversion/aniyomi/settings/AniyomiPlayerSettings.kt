package eu.kanade.tachiyomi.data.backup.conversion.aniyomi.settings

import app.katari.entry.interactions.player.PlaybackSpeed
import app.katari.entry.interactions.settings.AnimeMarkerSkipMode
import app.katari.entry.interactions.settings.AnimePlayerDevicePreferences
import app.katari.entry.interactions.settings.AnimePlayerOrientation
import app.katari.entry.interactions.settings.AnimePlayerPreferences
import eu.kanade.tachiyomi.data.backup.models.BooleanPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.FloatPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import tachiyomi.i18n.*
import kotlin.math.abs
import kotlin.math.roundToInt

/** Aniyomi's player settings Katari has its own of, which Aniyomi's Player settings and the video player set. */
internal fun AniyomiSettings.convertPlayerSettings() {
    take("pref_default_player_orientation_type_key")?.let { value ->
        // Older Aniyomi kept a screen orientation number here, which a later Aniyomi replaced
        val orientation = ORIENTATIONS[(value as? StringPreferenceValue)?.value] ?: return@let
        write(AnimePlayerPreferences.ORIENTATION_KEY, StringPreferenceValue(orientation.name))
    }
    take("pref_remember_brightness")?.let { write(AnimePlayerDevicePreferences.REMEMBER_BRIGHTNESS_KEY, it) }
    convertMarkerSkip()
    (take("pref_enable_ani_skip") as? BooleanPreferenceValue)?.let { aniSkip ->
        // Agreeing to AniSkip isn't agreeing to TheIntroDB (D064)
        if (aniSkip.value) {
            writeUnlisted(AnimePlayerPreferences.INTRO_TIMES_FROM_ANISKIP_KEY, BooleanPreferenceValue(true))
            writeUnlisted(AnimePlayerPreferences.INTRO_TIMES_FROM_THEINTRODB_KEY, BooleanPreferenceValue(false))
        }
        write(
            AnimePlayerPreferences.FIND_INTRO_TIMES_ONLINE_KEY,
            aniSkip,
            MR.strings.backup_conversion_aniyomi_aniskip_only.takeIf { aniSkip.value },
        )
    }
    (take("pref_pip_on_exit") as? BooleanPreferenceValue)?.let { onExit ->
        // Aniyomi's switch for picture-in-picture as a whole stays listed: Katari has none
        val pipEnabled = (peek("pref_enable_pip") as? BooleanPreferenceValue)?.value ?: true
        write(
            AnimePlayerPreferences.AUTO_ENTER_PICTURE_IN_PICTURE_KEY,
            BooleanPreferenceValue(onExit.value && pipEnabled),
        )
    }
    (take("pref_player_speed") as? FloatPreferenceValue)?.let { speed ->
        val percent = PlaybackSpeed.nearest((speed.value * 100).roundToInt()).hundredths
        write(AnimePlayerPreferences.DEFAULT_PLAYBACK_SPEED_PERCENT_KEY, IntPreferenceValue(percent))
    }
    take("invert_duration")?.let { write(AnimePlayerPreferences.SHOWS_REMAINING_TIME_KEY, it) }
    take("pref_auto_play_enabled")?.let { write(AnimePlayerPreferences.AUTO_PLAY_NEXT_KEY, it) }
    (take("pref_skip_length_preference") as? IntPreferenceValue)?.let { seconds ->
        val step = AnimePlayerPreferences.SEEK_STEP_SECONDS_OPTIONS.minBy { abs(it - seconds.value) }
        write(AnimePlayerPreferences.SEEK_STEP_SECONDS_KEY, IntPreferenceValue(step))
    }
}

/**
 * Aniyomi's three intro skip switches as Katari's one choice, in the order Aniyomi's player reads them: off, then
 * Netflix-style countdown, then skipping by itself, else the button.
 */
private fun AniyomiSettings.convertMarkerSkip() {
    val enabled = take("pref_enable_skip_intro") as? BooleanPreferenceValue
    val autoSkip = take("pref_enable_auto_skip_ani_skip") as? BooleanPreferenceValue
    val countdown = take("pref_enable_netflixStyle_aniskip") as? BooleanPreferenceValue
    if (enabled == null && autoSkip == null && countdown == null) return
    val mode = when {
        enabled?.value == false -> AnimeMarkerSkipMode.OFF
        countdown?.value == true -> AnimeMarkerSkipMode.COUNTDOWN
        autoSkip?.value == true -> AnimeMarkerSkipMode.AUTOMATIC
        else -> AnimeMarkerSkipMode.BUTTON
    }
    write(AnimePlayerPreferences.MARKER_SKIP_KEY, StringPreferenceValue(mode.name))
}

// Aniyomi's PlayerOrientation names: Katari turns the screen either way within portrait or landscape
private val ORIENTATIONS = mapOf(
    "Free" to AnimePlayerOrientation.SYSTEM,
    "Video" to AnimePlayerOrientation.FOLLOW_VIDEO,
    "Portrait" to AnimePlayerOrientation.PORTRAIT,
    "ReversePortrait" to AnimePlayerOrientation.PORTRAIT,
    "SensorPortrait" to AnimePlayerOrientation.PORTRAIT,
    "Landscape" to AnimePlayerOrientation.LANDSCAPE,
    "ReverseLandscape" to AnimePlayerOrientation.LANDSCAPE,
    "SensorLandscape" to AnimePlayerOrientation.LANDSCAPE,
)
