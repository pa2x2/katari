package app.katari.sync.client.setting.scope

/** The look of the app and its home screen: `UiPreferences`, `CustomPreferences` and the desktop's display settings. */
internal val AppearanceScopes = PreferenceScopeTable {
    // UiPreferences
    person(
        "pref_theme_mode_key",
        "relative_time_v2",
        "app_date_format",
        "app_time_format",
        "pref_render_images_description",
    )
    // Dynamic colour exists only where the system offers it, and pure black is for OLED screens.
    device("pref_app_theme", "pref_theme_dark_amoled_key", "tablet_ui_mode")

    // CustomPreferences
    person(
        "home_screen_startup_tab",
        "home_screen_tabs",
        "home_screen_tab_order",
        "home_screen_primary_tabs",
        "enable_feeds",
        "browse_long_press_action",
        "browse_long_press_action_overrides",
    )
    // The tab arrangement being edited, until it is saved.
    device("home_screen_navigation_draft")

    // Desktop: InterfaceSizePreferences, DisplayBackendPreferences, PreferenceAppLocaleStore. The app language lives
    // in the store all profiles share, and Android keeps its own in the system's per-app language.
    device("interface_size", "use_native_wayland", "app_language")
}
