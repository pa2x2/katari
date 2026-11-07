package app.katari.sync.client.setting.scope

/**
 * The device itself: `NetworkPreferences`, `PrivacyPreferences`, `ProgressiveImagePreferences`, `AppUpdatePreferences`
 * and Android's `BasePreferences` and extension installer.
 */
internal val SystemScopes = PreferenceScopeTable {
    device("redact_sensitive_headers", "verbose_logging", "doh_provider", "default_user_agent")
    device("crashlytics", "analytics")
    device("progressive_image_loading")
    device("app_update_check_on_launch", "app_update_channel")
    device("pref_hardware_bitmap_threshold", "extension_installer")
}
