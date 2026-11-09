package app.katari.sync.client.setting.scope

/** The look of the app and its home screen: `UiPreferences`, `CustomPreferences` and the desktop's display settings. */
internal val AppearanceScopes = PreferenceScopeTable(SettingsArea.APPEARANCE) {
    // UiPreferences
    person(
        "pref_theme_mode_key",
        "relative_time_v2",
        "app_date_format",
        "app_time_format",
        "pref_render_images_description",
    )
    // Dynamic colour exists only where the system offers it. Pure black is for OLED screens and the tablet layout for
    // large ones, which a person may have more than one of.
    device(DeviceBound.SYSTEM, "pref_app_theme")
    deviceByDefault("pref_theme_dark_amoled_key", "tablet_ui_mode")

    // CustomPreferences: the home screen's tabs here, and what Browse shows and does in its own area
    person("home_screen_startup_tab", "home_screen_tabs", "home_screen_tab_order", "home_screen_primary_tabs")
    area(SettingsArea.BROWSE) {
        person("enable_feeds", "browse_long_press_action", "browse_long_press_action_overrides")
    }
    // The tab arrangement being edited, until it is saved.
    device(DeviceBound.APP, "home_screen_navigation_draft")

    // Desktop: InterfaceSizePreferences, PreferenceAppLocaleStore. The interface size suits the screen. The app
    // language lives in the store all profiles share, and Android keeps its own in the system's per-app language.
    deviceByDefault("interface_size")
    device(DeviceBound.SYSTEM, "app_language")
}
