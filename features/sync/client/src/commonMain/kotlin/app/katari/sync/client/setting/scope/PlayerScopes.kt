package app.katari.sync.client.setting.scope

/**
 * The video player: `AnimePlayerPreferences` and `AnimePlayerDevicePreferences`. Playback habits sync; picture-in-
 * picture, background playback, orientation, quality and brightness belong to the device and its screen.
 */
internal val PlayerScopes = PreferenceScopeTable {
    person(
        "enable_anime_seek_preview",
        "anime_seek_step_seconds",
        "anime_auto_play_next",
        "anime_marker_skip",
        "anime_find_intro_times_online",
        "anime_intro_times_from_aniskip",
        "anime_intro_times_from_theintrodb",
        "anime_default_playback_speed_percent",
        "anime_shows_remaining_time",
        "anime_subtitles_in_picture",
    )
    device(
        "enable_anime_picture_in_picture",
        "anime_background_playback",
        "anime_preferred_quality_height",
        "anime_player_orientation",
        "anime_gesture_guide_shown_seek_swipe",
        "anime_player_remember_brightness",
    )
}
