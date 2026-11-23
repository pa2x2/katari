package app.katari.sync.ui.arrival.change.named

import app.katari.entry.interactions.settings.AnimePlayerPreferences
import tachiyomi.i18n.*

/** The player's settings it changes itself: the time label switches to the time left, the subtitle editor's placement. */
internal val playerNamedSettings = listOf(
    switchSetting(MR.strings.anime_playback_show_remaining_time) { AnimePlayerPreferences(it).showsRemainingTime },
    switchSetting(MR.strings.sync_setting_subtitles_in_picture) { AnimePlayerPreferences(it).subtitlesInPicture },
)
